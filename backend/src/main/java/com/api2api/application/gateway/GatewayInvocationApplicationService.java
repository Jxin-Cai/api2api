package com.api2api.application.gateway;

import com.api2api.application.BusinessException;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.gateway.model.GatewayInvocationResult;
import com.api2api.domain.gateway.model.InvocationError;
import com.api2api.domain.gateway.model.InvocationErrorType;
import com.api2api.domain.gateway.model.InvocationStatus;
import com.api2api.domain.gateway.service.GatewayInvocationService;
import com.api2api.domain.protocol.model.ConversionPayload;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.protocol.model.ConversionResult;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.protocol.repository.ProtocolConversionDefinitionRepository;
import com.api2api.domain.protocol.service.ProtocolConversionService;
import com.api2api.domain.routing.model.FailoverAction;
import com.api2api.domain.routing.model.FailoverDecision;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.model.RouteFailure;
import com.api2api.domain.routing.model.RouteFailureType;
import com.api2api.domain.routing.model.RoutePlan;
import com.api2api.domain.routing.model.RoutingRequest;
import com.api2api.domain.routing.service.RoutingPolicyService;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import java.time.Clock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GatewayInvocationApplicationService {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;
    private static final Duration RATE_LIMIT_ISOLATION_DURATION = Duration.ofHours(24);

    @NonNull
    private final ApiCredentialRepository apiCredentialRepository;

    @NonNull
    private final UsageRecordRepository usageRecordRepository;

    @NonNull
    private final ProviderChannelRepository providerChannelRepository;

    @NonNull
    private final ProtocolConversionDefinitionRepository conversionDefinitionRepository;

    @NonNull
    private final RoutingPolicyService routingPolicyService;

    @NonNull
    private final ProtocolConversionService protocolConversionService;

    @NonNull
    private final GatewayInvocationService gatewayInvocationService;

    @NonNull
    private final ProviderGatewayCallPort providerGatewayCallPort;

    @NonNull
    private final GatewayPayloadModelMappingPort payloadModelMappingPort;

    @NonNull
    private final GatewayStreamingConversionPort streamingConversionPort;

    @NonNull
    private final Clock clock;

    public GatewayInvocation invoke(InvokeGatewayCommand command) {
        return invokeOutcome(command).invocation();
    }

    public GatewayInvocationOutcome invokeOutcome(InvokeGatewayCommand command) {
        Objects.requireNonNull(command, "Invoke gateway command must not be null");

        PreparedRoute route = prepareRoute(command);
        GatewayInvocation invocation = route.invocation();
        RoutePlan routePlan = route.routePlan();

        if (!routePlan.hasCandidate()) {
            invocation = completeNoAvailableChannel(command, invocation);
            return GatewayInvocationOutcome.withoutProviderResponse(invocation);
        }

        invocation = gatewayInvocationService.route(invocation, routePlan, Instant.now(clock));
        RouteCandidate candidate = routePlan.firstCandidate();
        while (candidate != null) {
            SynchronousRouteAttempt attempt = invokeSynchronousRoute(command, route, invocation, candidate);
            invocation = attempt.invocation();
            if (attempt.retryNext()) {
                candidate = attempt.nextCandidate();
                continue;
            }
            return attempt.outcome();
        }

        invocation = completeAllCandidatesFailed(command, invocation);
        return GatewayInvocationOutcome.withoutProviderResponse(invocation);
    }

    public GatewayStreamingInvocation openStreaming(InvokeGatewayCommand command) {
        Objects.requireNonNull(command, "Invoke gateway command must not be null");
        if (!command.isStreaming()) {
            throw new IllegalArgumentException("Streaming invocation requires a streaming command");
        }

        PreparedRoute route = prepareRoute(command);
        GatewayInvocation invocation = route.invocation();
        RoutePlan routePlan = route.routePlan();

        if (!routePlan.hasCandidate()) {
            invocation = completeNoAvailableChannel(command, invocation);
            return GatewayStreamingInvocation.failed(invocation, command.getUsageRecordId());
        }

        invocation = gatewayInvocationService.route(invocation, routePlan, Instant.now(clock));
        RouteCandidate candidate = routePlan.firstCandidate();
        while (candidate != null) {
            StreamingRouteAttempt attempt = openStreamingRoute(command, route, invocation, candidate);
            invocation = attempt.invocation();
            if (attempt.retryNext()) {
                candidate = attempt.nextCandidate();
                continue;
            }
            return attempt.streamingInvocation();
        }

        invocation = completeAllCandidatesFailed(command, invocation);
        return GatewayStreamingInvocation.failed(invocation, command.getUsageRecordId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeStreamingSuccess(GatewayStreamingInvocation streamingInvocation) {
        completeStreamingSuccess(streamingInvocation, UnifiedTokenUsage.unknown());
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeStreamingSuccess(GatewayStreamingInvocation streamingInvocation, UnifiedTokenUsage usage) {
        Objects.requireNonNull(streamingInvocation, "Streaming invocation must not be null");
        if (!streamingInvocation.opened() || streamingInvocation.invocation().isTerminal()) {
            return;
        }
        UnifiedTokenUsage finalUsage = usage == null ? UnifiedTokenUsage.unknown() : usage;
        GatewayInvocation invocation = gatewayInvocationService.completeSuccess(
                streamingInvocation.invocation(),
                streamingInvocation.candidate(),
                finalUsage,
                true,
                Instant.now(clock)
        );
        log.info(
                "Gateway streaming request completed, requestId: {}, channelId: {}, usageKnown: {}, totalTokens: {}",
                invocation.requestId().value(),
                streamingInvocation.candidate().providerChannelId().value(),
                finalUsage.usageKnown(),
                finalUsage.usageKnown() ? finalUsage.totalTokens() : null
        );
        appendUsageRecord(streamingInvocation.usageRecordId(), invocation);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeStreamingFailure(GatewayStreamingInvocation streamingInvocation, RuntimeException exception) {
        Objects.requireNonNull(streamingInvocation, "Streaming invocation must not be null");
        if (!streamingInvocation.opened() || streamingInvocation.invocation().isTerminal()) {
            return;
        }
        InvocationError error = InvocationError.of(
                InvocationErrorType.UPSTREAM_FAILED,
                safeReason(exception),
                streamingInvocation.invocation().failures()
        );
        GatewayInvocation invocation = gatewayInvocationService.completeFailure(
                streamingInvocation.invocation(),
                error,
                true,
                Instant.now(clock)
        );
        log.warn(
                "Gateway streaming request failed, requestId: {}, channelId: {}, reason: {}",
                invocation.requestId().value(),
                streamingInvocation.candidate().providerChannelId().value(),
                error.message()
        );
        appendUsageRecord(streamingInvocation.usageRecordId(), invocation);
    }

    @Transactional(rollbackFor = Exception.class)
    public GatewayInvocation authenticateAndStartInvocation(InvokeGatewayCommand command) {
        Objects.requireNonNull(command, "Invoke gateway command must not be null");
        Instant now = Instant.now(clock);
        ConversionRequirement requirement = ConversionRequirement.of(
                command.isStreaming(),
                command.isToolCallingRequired(),
                command.isReasoningRequired()
        );
        ApiCredential credential = apiCredentialRepository.findByKeyHash(command.getKeyHash())
                .orElseThrow(() -> new BusinessException("API_CREDENTIAL_INVALID"));
        credential.assertUsable();
        credential.assertModelAllowed(command.getRequestedCredentialModel());
        BigDecimal currentConsumedTokens = usageRecordRepository.sumActualTokensByApiCredential(credential.getId());
        if (currentConsumedTokens.signum() < 0) {
            throw new BusinessException("INVALID_TOKEN_TOTAL");
        }
        credential.assertQuotaAvailable(currentConsumedTokens);
        GatewayInvocation invocation = GatewayInvocation.start(
                command.getGatewayInvocationId(),
                command.getGatewayRequestId(),
                credential.getOwnerUserId(),
                credential.getId(),
                command.getRequestProtocol(),
                command.getRequestedModel(),
                requirement,
                now
        );
        invocation = gatewayInvocationService.authenticate(invocation, credential, currentConsumedTokens, now);
        credential.markUsed(now);
        apiCredentialRepository.save(credential);
        return invocation;
    }

    @Transactional(rollbackFor = Exception.class)
    public void appendUsageRecord(UsageRecordId usageRecordId, GatewayInvocation invocation) {
        Objects.requireNonNull(usageRecordId, "Usage record id must not be null");
        Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        if (!hasBillableSuccessfulUsage(invocation)) {
            return;
        }
        UsageRecord usageRecord = UsageRecord.fromInvocation(usageRecordId, invocation, Instant.now(clock));
        usageRecordRepository.save(usageRecord);
    }

    private boolean hasBillableSuccessfulUsage(GatewayInvocation invocation) {
        if (!invocation.isTerminal()) {
            return false;
        }
        GatewayInvocationResult result = invocation.result();
        if (result == null || result.status() != InvocationStatus.SUCCESS) {
            return false;
        }
        UnifiedTokenUsage usage = result.usage();
        return usage != null && usage.usageKnown() && usage.totalTokens() > 0;
    }

    private PreparedRoute prepareRoute(InvokeGatewayCommand command) {
        GatewayInvocation invocation = authenticateAndStartInvocation(command);
        Instant now = Instant.now(clock);
        List<ProtocolConversionDefinition> conversionDefinitions = conversionDefinitionRepository.findAll();
        List<ProviderChannel> channels = routableChannels(now);
        RoutingRequest routingRequest = RoutingRequest.of(
                command.getRequestProtocol(),
                command.getRequestedModel(),
                invocation.requirement()
        );
        RoutePlan routePlan = routingPolicyService.buildRoutePlan(routingRequest, channels, conversionDefinitions, now);
        return new PreparedRoute(invocation, routePlan, conversionDefinitions);
    }

    private SynchronousRouteAttempt invokeSynchronousRoute(
            InvokeGatewayCommand command,
            PreparedRoute route,
            GatewayInvocation invocation,
            RouteCandidate candidate
    ) {
        try {
            SynchronousProviderCall providerCall = forwardSynchronousRequest(command, route, invocation, candidate);
            invocation = providerCall.invocation();
            ProviderGatewayResponse upstreamResponse = providerCall.response();
            if (!upstreamResponse.successful()) {
                RouteFailure routeFailure = toRouteFailure(candidate, upstreamResponse);
                isolateRateLimitedChannel(routeFailure);
                return handleSynchronousRouteFailure(
                        command,
                        invocation,
                        route.routePlan(),
                        routeFailure,
                        InvocationErrorType.UPSTREAM_FAILED,
                        upstreamResponse
                );
            }
            return SynchronousRouteAttempt.terminal(completeSynchronousSuccess(
                    command,
                    route,
                    invocation,
                    candidate,
                    upstreamResponse
            ));
        } catch (ProtocolConversionException exception) {
            RouteFailure routeFailure = toRouteFailure(candidate, RouteFailureType.CONVERSION_ERROR, exception);
            return handleSynchronousRouteFailure(
                    command,
                    invocation,
                    route.routePlan(),
                    routeFailure,
                    InvocationErrorType.CONVERSION_FAILED,
                    null
            );
        } catch (UpstreamGatewayException exception) {
            RouteFailure routeFailure = toRouteFailure(candidate, exception);
            isolateRateLimitedChannel(routeFailure);
            return handleSynchronousRouteFailure(
                    command,
                    invocation,
                    route.routePlan(),
                    routeFailure,
                    InvocationErrorType.UPSTREAM_FAILED,
                    null
            );
        } catch (RuntimeException exception) {
            RouteFailure routeFailure = toRouteFailure(candidate, mapUpstreamFailureType(exception), exception);
            return handleSynchronousRouteFailure(
                    command,
                    invocation,
                    route.routePlan(),
                    routeFailure,
                    InvocationErrorType.UPSTREAM_FAILED,
                    null
            );
        }
    }

    private SynchronousProviderCall forwardSynchronousRequest(
            InvokeGatewayCommand command,
            PreparedRoute route,
            GatewayInvocation invocation,
            RouteCandidate candidate
    ) {
        invocation.startAttempt(candidate, Instant.now(clock));
        logRouteAttempt(command, candidate);
        ConversionPayload requestPayload = ConversionPayload.of(
                command.getRequestProtocol(),
                requestBodyForCandidate(command, candidate),
                command.isStreaming()
        );
        ConversionResult requestConversion = protocolConversionService.convertRequest(
                requestPayload,
                candidate.upstreamProtocol(),
                conversionRequirement(invocation, candidate),
                route.conversionDefinitions()
        );
        GatewayInvocation convertedInvocation = gatewayInvocationService.recordConversion(
                invocation,
                requestConversion,
                true,
                Instant.now(clock)
        );
        ProviderGatewayResponse upstreamResponse = forwardToProvider(
                providerGatewayCallPort,
                candidate,
                requestConversion.body(),
                command.isStreaming(),
                command.getIncomingHeaders()
        );
        return new SynchronousProviderCall(convertedInvocation, upstreamResponse);
    }

    private GatewayInvocationOutcome completeSynchronousSuccess(
            InvokeGatewayCommand command,
            PreparedRoute route,
            GatewayInvocation invocation,
            RouteCandidate candidate,
            ProviderGatewayResponse upstreamResponse
    ) {
        ConversionPayload upstreamResponsePayload = ConversionPayload.of(
                upstreamResponse.protocol(),
                upstreamResponse.body(),
                upstreamResponse.streaming()
        );
        ConversionResult responseConversion = protocolConversionService.convertResponse(
                upstreamResponsePayload,
                command.getRequestProtocol(),
                conversionRequirement(invocation, candidate),
                route.conversionDefinitions()
        );
        responseConversion = rewriteResponseModel(candidate, responseConversion);
        invocation = gatewayInvocationService.recordConversion(
                invocation,
                responseConversion,
                false,
                Instant.now(clock)
        );
        UnifiedTokenUsage usage = responseConversion.usage().orElseGet(UnifiedTokenUsage::unknown);
        invocation = gatewayInvocationService.completeSuccess(
                invocation,
                candidate,
                usage,
                command.isStreaming(),
                Instant.now(clock)
        );
        appendUsageRecord(command.getUsageRecordId(), invocation);
        return GatewayInvocationOutcome.of(invocation, upstreamResponse);
    }

    private SynchronousRouteAttempt handleSynchronousRouteFailure(
            InvokeGatewayCommand command,
            GatewayInvocation invocation,
            RoutePlan routePlan,
            RouteFailure routeFailure,
            InvocationErrorType errorType,
            ProviderGatewayResponse upstreamResponse
    ) {
        FailoverResult failover = handleFailoverException(command, invocation, routePlan, routeFailure, errorType);
        if (failover.retryNext()) {
            return SynchronousRouteAttempt.retryNext(failover.invocation(), failover.nextCandidate());
        }
        GatewayInvocationOutcome outcome = upstreamResponse == null
                ? GatewayInvocationOutcome.withoutProviderResponse(failover.invocation())
                : GatewayInvocationOutcome.of(failover.invocation(), upstreamResponse);
        return SynchronousRouteAttempt.terminal(outcome);
    }

    private StreamingRouteAttempt openStreamingRoute(
            InvokeGatewayCommand command,
            PreparedRoute route,
            GatewayInvocation invocation,
            RouteCandidate candidate
    ) {
        try {
            StreamingProviderCall providerCall = openProviderStream(command, route, invocation, candidate);
            GatewayStreamingInvocation streamingInvocation = GatewayStreamingInvocation.opened(
                    providerCall.invocation(),
                    command.getUsageRecordId(),
                    candidate,
                    providerCall.response()
            );
            return StreamingRouteAttempt.terminal(streamingInvocation);
        } catch (ProtocolConversionException exception) {
            RouteFailure routeFailure = toRouteFailure(candidate, RouteFailureType.CONVERSION_ERROR, exception);
            return handleStreamingRouteFailure(
                    command,
                    invocation,
                    route.routePlan(),
                    routeFailure,
                    InvocationErrorType.CONVERSION_FAILED
            );
        } catch (UpstreamGatewayException exception) {
            RouteFailure routeFailure = toRouteFailure(candidate, exception);
            isolateRateLimitedChannel(routeFailure);
            return handleStreamingRouteFailure(
                    command,
                    invocation,
                    route.routePlan(),
                    routeFailure,
                    InvocationErrorType.UPSTREAM_FAILED
            );
        } catch (RuntimeException exception) {
            RouteFailure routeFailure = toRouteFailure(candidate, mapUpstreamFailureType(exception), exception);
            return handleStreamingRouteFailure(
                    command,
                    invocation,
                    route.routePlan(),
                    routeFailure,
                    InvocationErrorType.UPSTREAM_FAILED
            );
        }
    }

    private StreamingProviderCall openProviderStream(
            InvokeGatewayCommand command,
            PreparedRoute route,
            GatewayInvocation invocation,
            RouteCandidate candidate
    ) {
        invocation.startAttempt(candidate, Instant.now(clock));
        logRouteAttempt(command, candidate);
        if (candidate.requiresProtocolConversion()
                && !streamingConversionPort.supports(candidate.upstreamProtocol(), command.getRequestProtocol())) {
            throw new ProtocolConversionException("STREAMING_PROTOCOL_TRANSFORM_NOT_SUPPORTED");
        }
        ConversionPayload requestPayload = ConversionPayload.of(
                command.getRequestProtocol(),
                requestBodyForCandidate(command, candidate),
                true
        );
        ConversionResult requestConversion = protocolConversionService.convertRequest(
                requestPayload,
                candidate.upstreamProtocol(),
                conversionRequirement(invocation, candidate),
                route.conversionDefinitions()
        );
        GatewayInvocation convertedInvocation = gatewayInvocationService.recordConversion(
                invocation,
                requestConversion,
                true,
                Instant.now(clock)
        );
        ProviderStreamingResponse providerResponse = providerGatewayCallPort.openStream(
                candidate,
                requestConversion.body(),
                command.getIncomingHeaders()
        );
        return new StreamingProviderCall(convertedInvocation, providerResponse);
    }

    private StreamingRouteAttempt handleStreamingRouteFailure(
            InvokeGatewayCommand command,
            GatewayInvocation invocation,
            RoutePlan routePlan,
            RouteFailure routeFailure,
            InvocationErrorType errorType
    ) {
        FailoverResult failover = handleFailoverException(command, invocation, routePlan, routeFailure, errorType);
        if (failover.retryNext()) {
            return StreamingRouteAttempt.retryNext(failover.invocation(), failover.nextCandidate());
        }
        return StreamingRouteAttempt.terminal(GatewayStreamingInvocation.failed(
                failover.invocation(),
                command.getUsageRecordId()
        ));
    }

    private FailoverResult handleFailoverException(
            InvokeGatewayCommand command,
            GatewayInvocation invocation,
            RoutePlan routePlan,
            RouteFailure routeFailure,
            InvocationErrorType errorType
    ) {
        invocation = recordAttemptFailure(invocation, routeFailure);
        FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
        if (decision.action() == FailoverAction.RETRY_NEXT) {
            return FailoverResult.retryNext(invocation, decision.nextCandidate());
        }
        invocation = completeFailedInvocation(command, invocation, errorType, decision);
        appendUsageRecord(command.getUsageRecordId(), invocation);
        return FailoverResult.stop(invocation);
    }

    private GatewayInvocation completeNoAvailableChannel(InvokeGatewayCommand command, GatewayInvocation invocation) {
        logNoAvailableChannel(command);
        InvocationError error = InvocationError.withoutFailures(
                InvocationErrorType.NO_AVAILABLE_CHANNEL,
                "NO_AVAILABLE_CHANNEL"
        );
        invocation = gatewayInvocationService.completeFailure(invocation, error, command.isStreaming(), Instant.now(clock));
        appendUsageRecord(command.getUsageRecordId(), invocation);
        return invocation;
    }

    private GatewayInvocation completeAllCandidatesFailed(InvokeGatewayCommand command, GatewayInvocation invocation) {
        InvocationError error = InvocationError.of(
                InvocationErrorType.UPSTREAM_FAILED,
                "ALL_CANDIDATE_CHANNELS_FAILED",
                invocation.failures()
        );
        invocation = gatewayInvocationService.completeFailure(invocation, error, command.isStreaming(), Instant.now(clock));
        appendUsageRecord(command.getUsageRecordId(), invocation);
        return invocation;
    }

    private String requestBodyForCandidate(InvokeGatewayCommand command, RouteCandidate candidate) {
        if (!candidate.requiresModelMapping()) {
            return command.getRequestBody();
        }
        return payloadModelMappingPort.rewriteModel(
                command.getRequestProtocol(),
                command.getRequestBody(),
                candidate.upstreamModel()
        );
    }

    private ConversionRequirement conversionRequirement(
            GatewayInvocation invocation,
            RouteCandidate candidate
    ) {
        return invocation.requirement().forRoute(
                candidate.providerChannelId().value(),
                candidate.upstreamModel().value()
        );
    }

    private ConversionResult rewriteResponseModel(RouteCandidate candidate, ConversionResult responseConversion) {
        if (!candidate.requiresModelMapping()) {
            return responseConversion;
        }
        String body = payloadModelMappingPort.rewriteModel(candidate.clientProtocol(), responseConversion.body(), candidate.requestedModel());
        return ConversionResult.of(
                responseConversion.sourceProtocol(),
                responseConversion.targetProtocol(),
                body,
                responseConversion.passthrough(),
                responseConversion.usage().orElse(null)
        );
    }

    private ProviderGatewayResponse forwardToProvider(
            ProviderGatewayCallPort port,
            RouteCandidate candidate,
            String upstreamRequestBody,
            boolean streaming,
            Map<String, List<String>> incomingHeaders
    ) {
        Objects.requireNonNull(port, "Provider gateway call port must not be null");
        Objects.requireNonNull(candidate, "Route candidate must not be null");
        Objects.requireNonNull(upstreamRequestBody, "Upstream request body must not be null");
        ProviderGatewayResponse response = port.forward(candidate, upstreamRequestBody, streaming, incomingHeaders);
        if (!candidate.upstreamProtocol().equals(response.protocol())) {
            throw new BusinessException("UPSTREAM_RESPONSE_PROTOCOL_MISMATCH");
        }
        return response;
    }

    private GatewayInvocation recordAttemptFailure(GatewayInvocation invocation, RouteFailure routeFailure) {
        invocation.recordAttemptFailure(routeFailure, Instant.now(clock));
        return invocation;
    }

    private GatewayInvocation completeFailedInvocation(
            InvokeGatewayCommand command,
            GatewayInvocation invocation,
            InvocationErrorType errorType,
            FailoverDecision decision
    ) {
        InvocationError error = InvocationError.of(errorType, failureMessage(decision), decision.failures());
        log.warn(
                "Gateway request failed, requestId: {}, protocol: {}, requestedModel: {}, streaming: {}, errorType: {}, reason: {}",
                command.getGatewayRequestId().value(),
                command.getRequestProtocol(),
                command.getRequestedModel().value(),
                command.isStreaming(),
                errorType,
                error.message()
        );
        return gatewayInvocationService.completeFailure(invocation, error, command.isStreaming(), Instant.now(clock));
    }

    private String failureMessage(FailoverDecision decision) {
        List<RouteFailure> failures = decision.failures();
        if (failures.isEmpty()) {
            return safeFailureMessage(decision.reason());
        }
        RouteFailure latestFailure = failures.get(failures.size() - 1);
        return safeFailureMessage(latestFailure.failureType() + ": " + latestFailure.reason());
    }

    private String safeFailureMessage(String message) {
        if (message.length() > MAX_FAILURE_REASON_LENGTH) {
            return message.substring(0, MAX_FAILURE_REASON_LENGTH);
        }
        return message;
    }

    private RouteFailure toRouteFailure(RouteCandidate candidate, UpstreamGatewayException exception) {
        return RouteFailure.of(
                candidate.providerChannelId(),
                exception.failureType(),
                safeReason(exception),
                exception.retryable(),
                Instant.now(clock)
        );
    }

    private List<ProviderChannel> routableChannels(Instant now) {
        providerChannelRepository.restoreRateLimitedBefore(now.minus(RATE_LIMIT_ISOLATION_DURATION), now);
        return providerChannelRepository.findEnabledForRouting();
    }

    private void isolateRateLimitedChannel(RouteFailure failure) {
        if (failure.failureType() != RouteFailureType.RATE_LIMITED) {
            return;
        }
        providerChannelRepository.markRateLimited(failure.providerChannelId(), failure.occurredAt());
        log.warn(
                "Provider channel isolated after upstream rate limit, channelId: {}, isolationHours: {}",
                failure.providerChannelId().value(),
                RATE_LIMIT_ISOLATION_DURATION.toHours()
        );
    }

    private RouteFailure toRouteFailure(RouteCandidate candidate, ProviderGatewayResponse response) {
        RouteFailureType failureType = routeFailureType(response.statusCode());
        return RouteFailure.of(
                candidate.providerChannelId(),
                failureType,
                statusFailureMessage(response.statusCode(), response.body()),
                failureType.isRetryableByDefault() || isModelUnavailable(response.statusCode(), response.body()),
                Instant.now(clock)
        );
    }

    private boolean isModelUnavailable(int statusCode, String responseBody) {
        if (statusCode != 404 || responseBody == null) {
            return false;
        }
        String normalized = responseBody.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("model_not_found")
                || normalized.contains("model not found")
                || normalized.contains("not supported by any configured account");
    }

    private void logRouteAttempt(InvokeGatewayCommand command, RouteCandidate candidate) {
        log.info(
                "Gateway route attempt, requestId: {}, channelId: {}, upstreamProtocol: {}, upstreamModel: {}, streaming: {}",
                command.getGatewayRequestId().value(),
                candidate.providerChannelId().value(),
                candidate.upstreamProtocol(),
                candidate.upstreamModel().value(),
                command.isStreaming()
        );
    }

    private void logNoAvailableChannel(InvokeGatewayCommand command) {
        log.warn(
                "Gateway route unavailable, requestId: {}, protocol: {}, requestedModel: {}, streaming: {}, reason: {}",
                command.getGatewayRequestId().value(),
                command.getRequestProtocol(),
                command.getRequestedModel().value(),
                command.isStreaming(),
                InvocationErrorType.NO_AVAILABLE_CHANNEL
        );
    }

    private RouteFailureType routeFailureType(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return RouteFailureType.AUTHORIZATION_ERROR;
        }
        if (statusCode == 429) {
            return RouteFailureType.RATE_LIMITED;
        }
        if (statusCode >= 500) {
            return RouteFailureType.CHANNEL_UNAVAILABLE;
        }
        return RouteFailureType.UPSTREAM_ERROR;
    }

    private String statusFailureMessage(int statusCode, String responseBody) {
        String message = "Upstream returned HTTP " + statusCode;
        if (responseBody == null || responseBody.isBlank()) {
            return message;
        }
        String compactBody = responseBody.replaceAll("\\s+", " ").trim();
        int maxBodyLength = 500;
        if (compactBody.length() > maxBodyLength) {
            compactBody = compactBody.substring(0, maxBodyLength) + "...";
        }
        return message + ": " + compactBody;
    }

    private RouteFailure toRouteFailure(RouteCandidate candidate, RouteFailureType failureType, RuntimeException exception) {
        return RouteFailure.withDefaultRetryable(
                candidate.providerChannelId(),
                failureType,
                safeReason(exception),
                Instant.now(clock)
        );
    }

    private RouteFailureType mapUpstreamFailureType(RuntimeException exception) {
        String reason = safeReason(exception).toUpperCase();
        if (reason.contains("TIMEOUT") || reason.contains("TIMED_OUT")) {
            return RouteFailureType.TIMEOUT;
        }
        if (reason.contains("RATE_LIMIT") || reason.contains("RATE LIMITED") || reason.contains("TOO_MANY_REQUESTS")) {
            return RouteFailureType.RATE_LIMITED;
        }
        if (reason.contains("UNAVAILABLE") || reason.contains("CHANNEL_UNAVAILABLE") || reason.contains("SERVICE_UNAVAILABLE")) {
            return RouteFailureType.CHANNEL_UNAVAILABLE;
        }
        return RouteFailureType.UPSTREAM_ERROR;
    }

    private String safeReason(RuntimeException exception) {
        if (exception == null) {
            return "UPSTREAM_ERROR";
        }
        String reason = exception.getMessage();
        if (reason == null || reason.isBlank()) {
            reason = exception.getClass().getSimpleName();
        }
        String trimmed = reason.trim();
        if (trimmed.length() > MAX_FAILURE_REASON_LENGTH) {
            return trimmed.substring(0, MAX_FAILURE_REASON_LENGTH);
        }
        return trimmed;
    }

    private record PreparedRoute(
            GatewayInvocation invocation,
            RoutePlan routePlan,
            List<ProtocolConversionDefinition> conversionDefinitions
    ) {
    }

    private record SynchronousProviderCall(GatewayInvocation invocation, ProviderGatewayResponse response) {
    }

    private record StreamingProviderCall(GatewayInvocation invocation, ProviderStreamingResponse response) {
    }

    private record FailoverResult(GatewayInvocation invocation, RouteCandidate nextCandidate) {
        private static FailoverResult retryNext(GatewayInvocation invocation, RouteCandidate nextCandidate) {
            return new FailoverResult(
                    invocation,
                    Objects.requireNonNull(nextCandidate, "Next route candidate must not be null")
            );
        }

        private static FailoverResult stop(GatewayInvocation invocation) {
            return new FailoverResult(invocation, null);
        }

        private boolean retryNext() {
            return nextCandidate != null;
        }
    }

    private record SynchronousRouteAttempt(
            GatewayInvocation invocation,
            RouteCandidate nextCandidate,
            GatewayInvocationOutcome outcome
    ) {
        private static SynchronousRouteAttempt retryNext(GatewayInvocation invocation, RouteCandidate nextCandidate) {
            return new SynchronousRouteAttempt(
                    invocation,
                    Objects.requireNonNull(nextCandidate, "Next route candidate must not be null"),
                    null
            );
        }

        private static SynchronousRouteAttempt terminal(GatewayInvocationOutcome outcome) {
            GatewayInvocationOutcome nonNullOutcome = Objects.requireNonNull(
                    outcome,
                    "Gateway invocation outcome must not be null"
            );
            return new SynchronousRouteAttempt(nonNullOutcome.invocation(), null, nonNullOutcome);
        }

        private boolean retryNext() {
            return nextCandidate != null;
        }
    }

    private record StreamingRouteAttempt(
            GatewayInvocation invocation,
            RouteCandidate nextCandidate,
            GatewayStreamingInvocation streamingInvocation
    ) {
        private static StreamingRouteAttempt retryNext(GatewayInvocation invocation, RouteCandidate nextCandidate) {
            return new StreamingRouteAttempt(
                    invocation,
                    Objects.requireNonNull(nextCandidate, "Next route candidate must not be null"),
                    null
            );
        }

        private static StreamingRouteAttempt terminal(GatewayStreamingInvocation streamingInvocation) {
            GatewayStreamingInvocation nonNullInvocation = Objects.requireNonNull(
                    streamingInvocation,
                    "Gateway streaming invocation must not be null"
            );
            return new StreamingRouteAttempt(nonNullInvocation.invocation(), null, nonNullInvocation);
        }

        private boolean retryNext() {
            return nextCandidate != null;
        }
    }
}
