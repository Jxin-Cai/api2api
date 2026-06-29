package com.api2api.domain.gateway.model;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.protocol.model.ConversionResult;
import com.api2api.domain.routing.model.RouteAttempt;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.model.RouteFailure;
import com.api2api.domain.routing.model.RoutePlan;
import com.api2api.domain.user.model.UserAccountId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root representing the domain process of handling one gateway request.
 */
public final class GatewayInvocation {

    private final GatewayInvocationId id;
    private final GatewayRequestId requestId;
    private final UserAccountId userAccountId;
    private final ApiCredentialId apiCredentialId;
    private final ProtocolType requestProtocol;
    private final ModelName requestedModel;
    private final ConversionRequirement requirement;
    private GatewayInvocationState state;
    private RoutePlan routePlan;
    private final List<RouteAttempt> attempts;
    private ConversionTrace conversionTrace;
    private GatewayInvocationResult result;
    private final Instant startedAt;
    private Instant endedAt;

    private GatewayInvocation(
            GatewayInvocationId id,
            GatewayRequestId requestId,
            UserAccountId userAccountId,
            ApiCredentialId apiCredentialId,
            ProtocolType requestProtocol,
            ModelName requestedModel,
            ConversionRequirement requirement,
            GatewayInvocationState state,
            RoutePlan routePlan,
            List<RouteAttempt> attempts,
            ConversionTrace conversionTrace,
            GatewayInvocationResult result,
            Instant startedAt,
            Instant endedAt
    ) {
        this.id = Objects.requireNonNull(id, "Gateway invocation id must not be null");
        this.requestId = Objects.requireNonNull(requestId, "Gateway request id must not be null");
        this.userAccountId = Objects.requireNonNull(userAccountId, "User account id must not be null");
        this.apiCredentialId = Objects.requireNonNull(apiCredentialId, "API credential id must not be null");
        this.requestProtocol = Objects.requireNonNull(requestProtocol, "Request protocol must not be null");
        this.requestedModel = Objects.requireNonNull(requestedModel, "Requested model must not be null");
        this.requirement = Objects.requireNonNull(requirement, "Conversion requirement must not be null");
        this.state = Objects.requireNonNull(state, "Gateway invocation state must not be null");
        this.routePlan = routePlan;
        Objects.requireNonNull(attempts, "Route attempts must not be null");
        this.attempts = new ArrayList<>(attempts);
        this.conversionTrace = conversionTrace;
        this.result = result;
        this.startedAt = Objects.requireNonNull(startedAt, "Started time must not be null");
        if (endedAt != null && endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Ended time must not be before started time");
        }
        this.endedAt = endedAt;
    }

    public static GatewayInvocation start(
            GatewayInvocationId id,
            GatewayRequestId requestId,
            UserAccountId userAccountId,
            ApiCredentialId apiCredentialId,
            ProtocolType requestProtocol,
            ModelName requestedModel,
            ConversionRequirement requirement,
            Instant now
    ) {
        return new GatewayInvocation(
                id,
                requestId,
                userAccountId,
                apiCredentialId,
                requestProtocol,
                requestedModel,
                requirement,
                GatewayInvocationState.CREATED,
                null,
                List.of(),
                null,
                null,
                Objects.requireNonNull(now, "Current time must not be null"),
                null
        );
    }

    public void markAuthenticated(Instant now) {
        requireState(GatewayInvocationState.CREATED, "Only created invocation can be authenticated");
        Objects.requireNonNull(now, "Current time must not be null");
        state = GatewayInvocationState.AUTHENTICATED;
    }

    public void attachRoutePlan(RoutePlan routePlan, Instant now) {
        requireState(GatewayInvocationState.AUTHENTICATED, "Only authenticated invocation can be routed");
        Objects.requireNonNull(now, "Current time must not be null");
        RoutePlan nonNullRoutePlan = Objects.requireNonNull(routePlan, "Route plan must not be null");
        if (!nonNullRoutePlan.hasCandidate()) {
            throw new IllegalArgumentException("Route plan must contain at least one candidate");
        }
        this.routePlan = nonNullRoutePlan;
        this.state = GatewayInvocationState.ROUTED;
    }

    public RouteAttempt startAttempt(RouteCandidate candidate, Instant now) {
        requireNotTerminal();
        if (state != GatewayInvocationState.ROUTED && state != GatewayInvocationState.FORWARDING) {
            throw new IllegalStateException("Route attempt can only start after routing or during forwarding");
        }
        RouteCandidate nonNullCandidate = Objects.requireNonNull(candidate, "Route candidate must not be null");
        ensureCandidateInRoutePlan(nonNullCandidate);
        RouteAttempt attempt = RouteAttempt.start(nonNullCandidate, attempts.size() + 1, Objects.requireNonNull(now, "Current time must not be null"));
        attempts.add(attempt);
        state = GatewayInvocationState.FORWARDING;
        return attempt;
    }

    public void recordAttemptFailure(RouteFailure failure, Instant endedAt) {
        requireNotTerminal();
        RouteFailure nonNullFailure = Objects.requireNonNull(failure, "Route failure must not be null");
        Objects.requireNonNull(endedAt, "Ended time must not be null");
        int latestAttemptIndex = attempts.size() - 1;
        if (latestAttemptIndex < 0) {
            throw new IllegalStateException("No route attempt is in progress");
        }
        RouteAttempt latestAttempt = attempts.get(latestAttemptIndex);
        if (latestAttempt.failed()) {
            throw new IllegalStateException("Latest route attempt has already failed");
        }
        attempts.set(latestAttemptIndex, latestAttempt.markFailed(nonNullFailure, endedAt));
        state = GatewayInvocationState.ROUTED;
    }

    public void recordRequestConversion(ConversionResult conversionResult, Instant now) {
        requireNotTerminal();
        Objects.requireNonNull(now, "Current time must not be null");
        ConversionResult nonNullResult = Objects.requireNonNull(conversionResult, "Conversion result must not be null");
        if (!nonNullResult.sourceProtocol().equals(requestProtocol)) {
            throw new IllegalArgumentException("Request conversion source protocol must equal request protocol");
        }
        conversionTrace = conversionTrace == null
                ? ConversionTrace.withRequestConversion(nonNullResult)
                : conversionTrace.withRequestConversionRecorded(nonNullResult);
        state = GatewayInvocationState.CONVERTING;
    }

    public void recordResponseConversion(ConversionResult conversionResult, Instant now) {
        requireNotTerminal();
        Objects.requireNonNull(now, "Current time must not be null");
        ConversionResult nonNullResult = Objects.requireNonNull(conversionResult, "Conversion result must not be null");
        if (!nonNullResult.targetProtocol().equals(requestProtocol)) {
            throw new IllegalArgumentException("Response conversion target protocol must equal request protocol");
        }
        conversionTrace = conversionTrace == null
                ? ConversionTrace.withResponseConversion(nonNullResult)
                : conversionTrace.withResponseConversionRecorded(nonNullResult);
        state = GatewayInvocationState.CONVERTING;
    }

    public void succeed(GatewayInvocationResult result, Instant endedAt) {
        requireNotTerminal();
        GatewayInvocationResult nonNullResult = Objects.requireNonNull(result, "Gateway invocation result must not be null");
        if (nonNullResult.status() != InvocationStatus.SUCCESS) {
            throw new IllegalArgumentException("Successful invocation must use SUCCESS result");
        }
        if (nonNullResult.finalCandidate() == null) {
            throw new IllegalArgumentException("Successful invocation must contain final route candidate");
        }
        finish(nonNullResult, GatewayInvocationState.SUCCEEDED, endedAt);
    }

    public void fail(GatewayInvocationResult result, Instant endedAt) {
        requireNotTerminal();
        GatewayInvocationResult nonNullResult = Objects.requireNonNull(result, "Gateway invocation result must not be null");
        if (nonNullResult.status() != InvocationStatus.FAILED) {
            throw new IllegalArgumentException("Failed invocation must use FAILED result");
        }
        if (nonNullResult.error() == null) {
            throw new IllegalArgumentException("Failed invocation must contain invocation error");
        }
        finish(nonNullResult, GatewayInvocationState.FAILED, endedAt);
    }

    public boolean isTerminal() {
        return state.isTerminal();
    }

    public Optional<RouteCandidate> finalCandidate() {
        if (result == null || result.status() != InvocationStatus.SUCCESS) {
            return Optional.empty();
        }
        return Optional.of(result.finalCandidate());
    }

    public List<RouteFailure> failures() {
        return attempts.stream()
                .map(RouteAttempt::failure)
                .filter(Objects::nonNull)
                .toList();
    }

    private void finish(GatewayInvocationResult result, GatewayInvocationState terminalState, Instant endedAt) {
        Objects.requireNonNull(endedAt, "Ended time must not be null");
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Ended time must not be before started time");
        }
        this.result = result;
        this.state = terminalState;
        this.endedAt = endedAt;
    }

    private void requireState(GatewayInvocationState expectedState, String message) {
        requireNotTerminal();
        if (state != expectedState) {
            throw new IllegalStateException(message);
        }
    }

    private void requireNotTerminal() {
        if (isTerminal()) {
            throw new IllegalStateException("Terminal invocation state cannot be changed");
        }
    }

    private void ensureCandidateInRoutePlan(RouteCandidate candidate) {
        if (routePlan == null) {
            throw new IllegalStateException("Route plan must be attached before starting route attempt");
        }
        boolean candidateExists = routePlan.candidates().stream().anyMatch(candidate::equals);
        if (!candidateExists) {
            throw new IllegalArgumentException("Route candidate must belong to attached route plan");
        }
    }

    public GatewayInvocationId id() {
        return id;
    }

    public GatewayRequestId requestId() {
        return requestId;
    }

    public UserAccountId userAccountId() {
        return userAccountId;
    }

    public ApiCredentialId apiCredentialId() {
        return apiCredentialId;
    }

    public ProtocolType requestProtocol() {
        return requestProtocol;
    }

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ConversionRequirement requirement() {
        return requirement;
    }

    public GatewayInvocationState state() {
        return state;
    }

    public RoutePlan routePlan() {
        return routePlan;
    }

    public List<RouteAttempt> attempts() {
        return List.copyOf(attempts);
    }

    public ConversionTrace conversionTrace() {
        return conversionTrace;
    }

    public GatewayInvocationResult result() {
        return result;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public GatewayInvocationId getId() {
        return id;
    }

    public GatewayRequestId getRequestId() {
        return requestId;
    }

    public UserAccountId getUserAccountId() {
        return userAccountId;
    }

    public ApiCredentialId getApiCredentialId() {
        return apiCredentialId;
    }

    public ProtocolType getRequestProtocol() {
        return requestProtocol;
    }

    public ModelName getRequestedModel() {
        return requestedModel;
    }

    public ConversionRequirement getRequirement() {
        return requirement;
    }

    public GatewayInvocationState getState() {
        return state;
    }

    public RoutePlan getRoutePlan() {
        return routePlan;
    }

    public List<RouteAttempt> getAttempts() {
        return attempts();
    }

    public ConversionTrace getConversionTrace() {
        return conversionTrace;
    }

    public GatewayInvocationResult getResult() {
        return result;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }
}
