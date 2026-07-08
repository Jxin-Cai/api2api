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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GatewayInvocationApplicationService {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

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
    private final Clock clock;

    public GatewayInvocation invoke(InvokeGatewayCommand command) {
        return invokeOutcome(command).invocation();
    }

    public GatewayInvocationOutcome invokeOutcome(InvokeGatewayCommand command) {
        Objects.requireNonNull(command, "Invoke gateway command must not be null");

        GatewayInvocation invocation = authenticateAndStartInvocation(command);
        Instant now = Instant.now(clock);
        List<ProtocolConversionDefinition> conversionDefinitions = conversionDefinitionRepository.findAll();
        List<ProviderChannel> channels = providerChannelRepository.findEnabledForRouting();
        RoutingRequest routingRequest = RoutingRequest.of(
                command.getRequestProtocol(),
                command.getRequestedModel(),
                invocation.requirement()
        );
        RoutePlan routePlan = routingPolicyService.buildRoutePlan(routingRequest, channels, conversionDefinitions, now);

        if (!routePlan.hasCandidate()) {
            InvocationError error = InvocationError.withoutFailures(
                    InvocationErrorType.NO_AVAILABLE_CHANNEL,
                    "NO_AVAILABLE_CHANNEL"
            );
            invocation = gatewayInvocationService.completeFailure(invocation, error, command.isStreaming(), Instant.now(clock));
            appendUsageRecord(command.getUsageRecordId(), invocation);
            return GatewayInvocationOutcome.withoutProviderResponse(invocation);
        }

        invocation = gatewayInvocationService.route(invocation, routePlan, Instant.now(clock));
        RouteCandidate candidate = routePlan.firstCandidate();
        while (candidate != null) {
            try {
                invocation.startAttempt(candidate, Instant.now(clock));
                ConversionPayload requestPayload = ConversionPayload.of(
                        command.getRequestProtocol(),
                        command.getRequestBody(),
                        command.isStreaming()
                );
                ConversionResult requestConversion = protocolConversionService.convertRequest(
                        requestPayload,
                        candidate.upstreamProtocol(),
                        invocation.requirement(),
                        conversionDefinitions
                );
                invocation = gatewayInvocationService.recordConversion(
                        invocation,
                        requestConversion,
                        true,
                        Instant.now(clock)
                );

                String upstreamRequestBody = rewriteRequestModel(candidate, requestConversion.body());
                ProviderGatewayResponse upstreamResponse = forwardToProvider(
                        providerGatewayCallPort,
                        candidate,
                        upstreamRequestBody,
                        command.isStreaming(),
                        command.getIncomingHeaders()
                );
                if (!upstreamResponse.successful()) {
                    RouteFailure routeFailure = toRouteFailure(candidate, upstreamResponse);
                    invocation = recordAttemptFailure(invocation, routeFailure);
                    FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
                    if (decision.action() == FailoverAction.RETRY_NEXT) {
                        candidate = decision.nextCandidate();
                        continue;
                    }
                    invocation = completeFailedInvocation(command, invocation, InvocationErrorType.UPSTREAM_FAILED, decision);
                    appendUsageRecord(command.getUsageRecordId(), invocation);
                    return GatewayInvocationOutcome.of(invocation, upstreamResponse);
                }
                ConversionPayload upstreamResponsePayload = ConversionPayload.of(
                        upstreamResponse.protocol(),
                        upstreamResponse.body(),
                        upstreamResponse.streaming()
                );
                ConversionResult responseConversion = protocolConversionService.convertResponse(
                        upstreamResponsePayload,
                        command.getRequestProtocol(),
                        invocation.requirement(),
                        conversionDefinitions
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
            } catch (ProtocolConversionException exception) {
                RouteFailure routeFailure = toRouteFailure(candidate, RouteFailureType.CONVERSION_ERROR, exception);
                invocation = recordAttemptFailure(invocation, routeFailure);
                FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
                if (decision.action() == FailoverAction.RETRY_NEXT) {
                    candidate = decision.nextCandidate();
                    continue;
                }
                invocation = completeFailedInvocation(command, invocation, InvocationErrorType.CONVERSION_FAILED, decision);
                appendUsageRecord(command.getUsageRecordId(), invocation);
                return GatewayInvocationOutcome.withoutProviderResponse(invocation);
            } catch (UpstreamGatewayException exception) {
                RouteFailure routeFailure = toRouteFailure(candidate, exception);
                invocation = recordAttemptFailure(invocation, routeFailure);
                FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
                if (decision.action() == FailoverAction.RETRY_NEXT) {
                    candidate = decision.nextCandidate();
                    continue;
                }
                invocation = completeFailedInvocation(command, invocation, InvocationErrorType.UPSTREAM_FAILED, decision);
                appendUsageRecord(command.getUsageRecordId(), invocation);
                return GatewayInvocationOutcome.withoutProviderResponse(invocation);
            } catch (RuntimeException exception) {
                RouteFailure routeFailure = toRouteFailure(candidate, mapUpstreamFailureType(exception), exception);
                invocation = recordAttemptFailure(invocation, routeFailure);
                FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
                if (decision.action() == FailoverAction.RETRY_NEXT) {
                    candidate = decision.nextCandidate();
                    continue;
                }
                invocation = completeFailedInvocation(command, invocation, InvocationErrorType.UPSTREAM_FAILED, decision);
                appendUsageRecord(command.getUsageRecordId(), invocation);
                return GatewayInvocationOutcome.withoutProviderResponse(invocation);
            }
        }

        InvocationError error = InvocationError.of(
                InvocationErrorType.UPSTREAM_FAILED,
                "ALL_CANDIDATE_CHANNELS_FAILED",
                invocation.failures()
        );
        invocation = gatewayInvocationService.completeFailure(invocation, error, command.isStreaming(), Instant.now(clock));
        appendUsageRecord(command.getUsageRecordId(), invocation);
        return GatewayInvocationOutcome.withoutProviderResponse(invocation);
    }

    public GatewayStreamingInvocation openStreaming(InvokeGatewayCommand command) {
        Objects.requireNonNull(command, "Invoke gateway command must not be null");
        if (!command.isStreaming()) {
            throw new IllegalArgumentException("Streaming invocation requires a streaming command");
        }

        GatewayInvocation invocation = authenticateAndStartInvocation(command);
        Instant now = Instant.now(clock);
        List<ProtocolConversionDefinition> conversionDefinitions = conversionDefinitionRepository.findAll();
        List<ProviderChannel> channels = providerChannelRepository.findEnabledForRouting();
        RoutingRequest routingRequest = RoutingRequest.of(
                command.getRequestProtocol(),
                command.getRequestedModel(),
                invocation.requirement()
        );
        RoutePlan routePlan = routingPolicyService.buildRoutePlan(routingRequest, channels, conversionDefinitions, now);

        if (!routePlan.hasCandidate()) {
            InvocationError error = InvocationError.withoutFailures(
                    InvocationErrorType.NO_AVAILABLE_CHANNEL,
                    "NO_AVAILABLE_CHANNEL"
            );
            invocation = gatewayInvocationService.completeFailure(invocation, error, true, Instant.now(clock));
            appendUsageRecord(command.getUsageRecordId(), invocation);
            return GatewayStreamingInvocation.failed(invocation, command.getUsageRecordId());
        }

        invocation = gatewayInvocationService.route(invocation, routePlan, Instant.now(clock));
        RouteCandidate candidate = routePlan.firstCandidate();
        while (candidate != null) {
            try {
                invocation.startAttempt(candidate, Instant.now(clock));
                if (candidate.requiresProtocolConversion()) {
                    throw new ProtocolConversionException("STREAMING_PROTOCOL_TRANSFORM_NOT_SUPPORTED");
                }
                ConversionPayload requestPayload = ConversionPayload.of(
                        command.getRequestProtocol(),
                        command.getRequestBody(),
                        true
                );
                ConversionResult requestConversion = protocolConversionService.convertRequest(
                        requestPayload,
                        candidate.upstreamProtocol(),
                        invocation.requirement(),
                        conversionDefinitions
                );
                invocation = gatewayInvocationService.recordConversion(
                        invocation,
                        requestConversion,
                        true,
                        Instant.now(clock)
                );
                ProviderStreamingResponse providerResponse = providerGatewayCallPort.openStream(
                        candidate,
                        rewriteRequestModel(candidate, requestConversion.body()),
                        command.getIncomingHeaders()
                );
                return GatewayStreamingInvocation.opened(
                        invocation,
                        command.getUsageRecordId(),
                        candidate,
                        providerResponse
                );
            } catch (ProtocolConversionException exception) {
                RouteFailure routeFailure = toRouteFailure(candidate, RouteFailureType.CONVERSION_ERROR, exception);
                invocation = recordAttemptFailure(invocation, routeFailure);
                FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
                if (decision.action() == FailoverAction.RETRY_NEXT) {
                    candidate = decision.nextCandidate();
                    continue;
                }
                invocation = completeFailedInvocation(command, invocation, InvocationErrorType.CONVERSION_FAILED, decision);
                appendUsageRecord(command.getUsageRecordId(), invocation);
                return GatewayStreamingInvocation.failed(invocation, command.getUsageRecordId());
            } catch (UpstreamGatewayException exception) {
                RouteFailure routeFailure = toRouteFailure(candidate, exception);
                invocation = recordAttemptFailure(invocation, routeFailure);
                FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
                if (decision.action() == FailoverAction.RETRY_NEXT) {
                    candidate = decision.nextCandidate();
                    continue;
                }
                invocation = completeFailedInvocation(command, invocation, InvocationErrorType.UPSTREAM_FAILED, decision);
                appendUsageRecord(command.getUsageRecordId(), invocation);
                return GatewayStreamingInvocation.failed(invocation, command.getUsageRecordId());
            } catch (RuntimeException exception) {
                RouteFailure routeFailure = toRouteFailure(candidate, mapUpstreamFailureType(exception), exception);
                invocation = recordAttemptFailure(invocation, routeFailure);
                FailoverDecision decision = routingPolicyService.decideNext(routePlan, invocation.attempts(), routeFailure);
                if (decision.action() == FailoverAction.RETRY_NEXT) {
                    candidate = decision.nextCandidate();
                    continue;
                }
                invocation = completeFailedInvocation(command, invocation, InvocationErrorType.UPSTREAM_FAILED, decision);
                appendUsageRecord(command.getUsageRecordId(), invocation);
                return GatewayStreamingInvocation.failed(invocation, command.getUsageRecordId());
            }
        }

        InvocationError error = InvocationError.of(
                InvocationErrorType.UPSTREAM_FAILED,
                "ALL_CANDIDATE_CHANNELS_FAILED",
                invocation.failures()
        );
        invocation = gatewayInvocationService.completeFailure(invocation, error, true, Instant.now(clock));
        appendUsageRecord(command.getUsageRecordId(), invocation);
        return GatewayStreamingInvocation.failed(invocation, command.getUsageRecordId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeStreamingSuccess(GatewayStreamingInvocation streamingInvocation) {
        Objects.requireNonNull(streamingInvocation, "Streaming invocation must not be null");
        if (!streamingInvocation.opened() || streamingInvocation.invocation().isTerminal()) {
            return;
        }
        GatewayInvocation invocation = gatewayInvocationService.completeSuccess(
                streamingInvocation.invocation(),
                streamingInvocation.candidate(),
                UnifiedTokenUsage.unknown(),
                true,
                Instant.now(clock)
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
        long currentConsumedTokens = usageRecordRepository.sumTotalTokensByApiCredential(credential.getId());
        if (currentConsumedTokens < 0) {
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

    private String rewriteRequestModel(RouteCandidate candidate, String body) {
        if (!candidate.requiresModelMapping()) {
            return body;
        }
        return payloadModelMappingPort.rewriteModel(candidate.upstreamProtocol(), body, candidate.upstreamModel());
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

    private RouteFailure toRouteFailure(RouteCandidate candidate, ProviderGatewayResponse response) {
        RouteFailureType failureType = routeFailureType(response.statusCode());
        return RouteFailure.of(
                candidate.providerChannelId(),
                failureType,
                statusFailureMessage(response.statusCode(), response.body()),
                failureType.isRetryableByDefault(),
                Instant.now(clock)
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
}
