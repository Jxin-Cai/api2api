package com.api2api.domain.credential.event;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialStatus;
import com.api2api.domain.user.model.UserAccountId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event emitted when an API credential status changes.
 */
public final class ApiCredentialStatusChangedEvent {

    private final ApiCredentialId apiCredentialId;
    private final UserAccountId ownerUserId;
    private final ApiCredentialStatus previousStatus;
    private final ApiCredentialStatus currentStatus;
    private final Instant occurredAt;

    private ApiCredentialStatusChangedEvent(
            ApiCredentialId apiCredentialId,
            UserAccountId ownerUserId,
            ApiCredentialStatus previousStatus,
            ApiCredentialStatus currentStatus,
            Instant occurredAt
    ) {
        this.apiCredentialId = Objects.requireNonNull(apiCredentialId, "API credential id must not be null");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "Owner user id must not be null");
        this.previousStatus = Objects.requireNonNull(previousStatus, "Previous status must not be null");
        this.currentStatus = Objects.requireNonNull(currentStatus, "Current status must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred time must not be null");
    }

    public static ApiCredentialStatusChangedEvent of(
            ApiCredentialId apiCredentialId,
            UserAccountId ownerUserId,
            ApiCredentialStatus previousStatus,
            ApiCredentialStatus currentStatus,
            Instant occurredAt
    ) {
        if (previousStatus == currentStatus) {
            throw new IllegalArgumentException("Status changed event requires different previous and current statuses");
        }
        return new ApiCredentialStatusChangedEvent(
                apiCredentialId,
                ownerUserId,
                previousStatus,
                currentStatus,
                occurredAt
        );
    }

    public ApiCredentialId getApiCredentialId() {
        return apiCredentialId;
    }

    public UserAccountId getOwnerUserId() {
        return ownerUserId;
    }

    public ApiCredentialStatus getPreviousStatus() {
        return previousStatus;
    }

    public ApiCredentialStatus getCurrentStatus() {
        return currentStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
