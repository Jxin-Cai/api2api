package com.api2api.domain.usage.event;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.user.model.UserAccountId;
import java.time.Instant;
import java.util.Objects;

/**
 * Event published after a usage record has been appended successfully.
 */
public final class UsageRecordCreatedEvent {

    private final UsageRecordId usageRecordId;
    private final GatewayRequestId requestId;
    private final UserAccountId userAccountId;
    private final ApiCredentialId apiCredentialId;
    private final long totalTokens;
    private final Instant occurredAt;

    public UsageRecordCreatedEvent(
            UsageRecordId usageRecordId,
            GatewayRequestId requestId,
            UserAccountId userAccountId,
            ApiCredentialId apiCredentialId,
            long totalTokens,
            Instant occurredAt
    ) {
        if (totalTokens < 0) {
            throw new IllegalArgumentException("Total tokens must not be negative");
        }
        this.usageRecordId = Objects.requireNonNull(usageRecordId, "Usage record id must not be null");
        this.requestId = Objects.requireNonNull(requestId, "Request id must not be null");
        this.userAccountId = Objects.requireNonNull(userAccountId, "User account id must not be null");
        this.apiCredentialId = Objects.requireNonNull(apiCredentialId, "API credential id must not be null");
        this.totalTokens = totalTokens;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred time must not be null");
    }

    public UsageRecordId usageRecordId() {
        return usageRecordId;
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

    public long totalTokens() {
        return totalTokens;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
