package com.api2api.domain.user.event;

import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserAccountStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event emitted when a user account status changes.
 */
public final class UserAccountStatusChangedEvent {

    private final UserAccountId userAccountId;
    private final UserAccountStatus previousStatus;
    private final UserAccountStatus currentStatus;
    private final Instant occurredAt;

    private UserAccountStatusChangedEvent(
            UserAccountId userAccountId,
            UserAccountStatus previousStatus,
            UserAccountStatus currentStatus,
            Instant occurredAt
    ) {
        this.userAccountId = Objects.requireNonNull(userAccountId, "User account id must not be null");
        this.previousStatus = Objects.requireNonNull(previousStatus, "Previous status must not be null");
        this.currentStatus = Objects.requireNonNull(currentStatus, "Current status must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred time must not be null");
    }

    public static UserAccountStatusChangedEvent of(
            UserAccountId userAccountId,
            UserAccountStatus previousStatus,
            UserAccountStatus currentStatus,
            Instant occurredAt
    ) {
        if (previousStatus == currentStatus) {
            throw new IllegalArgumentException("Status changed event requires different previous and current statuses");
        }
        return new UserAccountStatusChangedEvent(userAccountId, previousStatus, currentStatus, occurredAt);
    }

    public UserAccountId getUserAccountId() {
        return userAccountId;
    }

    public UserAccountStatus getPreviousStatus() {
        return previousStatus;
    }

    public UserAccountStatus getCurrentStatus() {
        return currentStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
