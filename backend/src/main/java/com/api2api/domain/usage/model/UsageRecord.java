package com.api2api.domain.usage.model;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.gateway.model.GatewayInvocationResult;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.gateway.model.InvocationStatus;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserRole;
import java.time.Instant;
import java.util.Objects;

/**
 * Append-only aggregate root that represents the immutable usage fact of one gateway invocation.
 */
public final class UsageRecord {

    private final UsageRecordId id;
    private final GatewayRequestId requestId;
    private final UserAccountId userAccountId;
    private final ApiCredentialId apiCredentialId;
    private final ModelName requestedModel;
    private final ModelName upstreamModel;
    private final ProtocolType requestProtocol;
    private final ProtocolType upstreamProtocol;
    private final ProviderChannelId providerChannelId;
    private final UsageRecordStatus status;
    private final UsageTokenBreakdown tokenUsage;
    private final boolean streaming;
    private final Instant startedAt;
    private final Instant endedAt;
    private final UsageDuration duration;
    private final UsageErrorDiagnostic errorDiagnostic;
    private final Instant createdAt;

    private UsageRecord(
            UsageRecordId id,
            GatewayRequestId requestId,
            UserAccountId userAccountId,
            ApiCredentialId apiCredentialId,
            ModelName requestedModel,
            ModelName upstreamModel,
            ProtocolType requestProtocol,
            ProtocolType upstreamProtocol,
            ProviderChannelId providerChannelId,
            UsageRecordStatus status,
            UsageTokenBreakdown tokenUsage,
            boolean streaming,
            Instant startedAt,
            Instant endedAt,
            UsageDuration duration,
            UsageErrorDiagnostic errorDiagnostic,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Usage record id must not be null");
        this.requestId = Objects.requireNonNull(requestId, "Gateway request id must not be null");
        this.userAccountId = Objects.requireNonNull(userAccountId, "User account id must not be null");
        this.apiCredentialId = Objects.requireNonNull(apiCredentialId, "API credential id must not be null");
        this.requestedModel = Objects.requireNonNull(requestedModel, "Requested model must not be null");
        this.upstreamModel = upstreamModel;
        this.requestProtocol = Objects.requireNonNull(requestProtocol, "Request protocol must not be null");
        this.upstreamProtocol = upstreamProtocol;
        this.providerChannelId = providerChannelId;
        this.status = Objects.requireNonNull(status, "Usage record status must not be null");
        this.tokenUsage = Objects.requireNonNull(tokenUsage, "Token usage must not be null");
        this.streaming = streaming;
        this.startedAt = Objects.requireNonNull(startedAt, "Started time must not be null");
        this.endedAt = Objects.requireNonNull(endedAt, "Ended time must not be null");
        if (this.endedAt.isBefore(this.startedAt)) {
            throw new IllegalArgumentException("Ended time must not be before started time");
        }
        this.duration = Objects.requireNonNull(duration, "Usage duration must not be null");
        UsageDuration calculatedDuration = UsageDuration.between(this.startedAt, this.endedAt);
        if (!this.duration.equals(calculatedDuration)) {
            throw new IllegalArgumentException("Usage duration must equal endedAt minus startedAt");
        }
        this.errorDiagnostic = errorDiagnostic;
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        validateStatusInvariant();
    }

    public static UsageRecord fromInvocation(UsageRecordId id, GatewayInvocation invocation, Instant now) {
        GatewayInvocation nonNullInvocation = Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        if (!nonNullInvocation.isTerminal()) {
            throw new IllegalArgumentException("Usage record can only be created from a terminal gateway invocation");
        }
        GatewayInvocationResult result = Objects.requireNonNull(
                nonNullInvocation.result(),
                "Terminal gateway invocation result must not be null"
        );
        Instant endedAt = Objects.requireNonNull(
                nonNullInvocation.endedAt(),
                "Terminal gateway invocation ended time must not be null"
        );
        UsageRecordStatus status = toUsageStatus(result.status());
        return new UsageRecord(
                id,
                nonNullInvocation.requestId(),
                nonNullInvocation.userAccountId(),
                nonNullInvocation.apiCredentialId(),
                nonNullInvocation.requestedModel(),
                result.upstreamModel(),
                nonNullInvocation.requestProtocol(),
                result.upstreamProtocol(),
                providerChannelIdOf(result),
                status,
                UsageTokenBreakdown.fromUnified(result.usage()),
                result.streaming(),
                nonNullInvocation.startedAt(),
                endedAt,
                UsageDuration.between(nonNullInvocation.startedAt(), endedAt),
                status == UsageRecordStatus.FAILED
                        ? UsageErrorDiagnostic.fromInvocationError(result.error())
                        : null,
                Objects.requireNonNull(now, "Current time must not be null")
        );
    }

    public static UsageRecord rehydrate(
            UsageRecordId id,
            GatewayRequestId requestId,
            UserAccountId userAccountId,
            ApiCredentialId apiCredentialId,
            ModelName requestedModel,
            ModelName upstreamModel,
            ProtocolType requestProtocol,
            ProtocolType upstreamProtocol,
            ProviderChannelId providerChannelId,
            UsageRecordStatus status,
            UsageTokenBreakdown tokenUsage,
            boolean streaming,
            Instant startedAt,
            Instant endedAt,
            UsageDuration duration,
            UsageErrorDiagnostic errorDiagnostic,
            Instant createdAt
    ) {
        return new UsageRecord(
                id,
                requestId,
                userAccountId,
                apiCredentialId,
                requestedModel,
                upstreamModel,
                requestProtocol,
                upstreamProtocol,
                providerChannelId,
                status,
                tokenUsage,
                streaming,
                startedAt,
                endedAt,
                duration,
                errorDiagnostic,
                createdAt
        );
    }

    private static UsageRecordStatus toUsageStatus(InvocationStatus invocationStatus) {
        InvocationStatus nonNullStatus = Objects.requireNonNull(invocationStatus, "Invocation status must not be null");
        return switch (nonNullStatus) {
            case SUCCESS -> UsageRecordStatus.SUCCESS;
            case FAILED -> UsageRecordStatus.FAILED;
        };
    }

    private static ProviderChannelId providerChannelIdOf(GatewayInvocationResult result) {
        if (result.finalCandidate() == null) {
            return null;
        }
        return result.finalCandidate().providerChannelId();
    }

    private void validateStatusInvariant() {
        if (status == UsageRecordStatus.SUCCESS) {
            if (upstreamModel == null || upstreamProtocol == null || providerChannelId == null) {
                throw new IllegalArgumentException("Successful usage record must include upstream model, upstream protocol and provider channel");
            }
            if (errorDiagnostic != null) {
                throw new IllegalArgumentException("Successful usage record must not include error diagnostic");
            }
            return;
        }
        if (status == UsageRecordStatus.FAILED && errorDiagnostic == null) {
            throw new IllegalArgumentException("Failed usage record must include error diagnostic");
        }
    }

    public long totalTokens() {
        return tokenUsage.totalTokens();
    }

    public boolean isSuccessful() {
        return status == UsageRecordStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == UsageRecordStatus.FAILED;
    }

    public boolean hasKnownUsage() {
        return tokenUsage.usageKnown();
    }

    public boolean visibleProviderChannelFor(UserRole viewerRole) {
        return Objects.requireNonNull(viewerRole, "Viewer role must not be null") == UserRole.ADMIN;
    }

    public void assertAppendOnly() {
        // This aggregate intentionally exposes no mutator. Repositories must persist it with append-only semantics.
    }

    public UsageRecordId id() {
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

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ModelName upstreamModel() {
        return upstreamModel;
    }

    public ProtocolType requestProtocol() {
        return requestProtocol;
    }

    public ProtocolType upstreamProtocol() {
        return upstreamProtocol;
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public UsageRecordStatus status() {
        return status;
    }

    public UsageTokenBreakdown tokenUsage() {
        return tokenUsage;
    }

    public boolean streaming() {
        return streaming;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public UsageDuration duration() {
        return duration;
    }

    public UsageErrorDiagnostic errorDiagnostic() {
        return errorDiagnostic;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public UsageRecordId getId() {
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

    public ModelName getRequestedModel() {
        return requestedModel;
    }

    public ModelName getUpstreamModel() {
        return upstreamModel;
    }

    public ProtocolType getRequestProtocol() {
        return requestProtocol;
    }

    public ProtocolType getUpstreamProtocol() {
        return upstreamProtocol;
    }

    public ProviderChannelId getProviderChannelId() {
        return providerChannelId;
    }

    public UsageRecordStatus getStatus() {
        return status;
    }

    public UsageTokenBreakdown getTokenUsage() {
        return tokenUsage;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public UsageDuration getDuration() {
        return duration;
    }

    public UsageErrorDiagnostic getErrorDiagnostic() {
        return errorDiagnostic;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
