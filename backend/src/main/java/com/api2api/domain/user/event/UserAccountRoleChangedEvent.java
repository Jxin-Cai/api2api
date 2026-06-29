package com.api2api.domain.user.event;

import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserRole;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event emitted when a user account role changes.
 */
public final class UserAccountRoleChangedEvent {

    private final UserAccountId userAccountId;
    private final UserRole previousRole;
    private final UserRole currentRole;
    private final Instant occurredAt;

    private UserAccountRoleChangedEvent(
            UserAccountId userAccountId,
            UserRole previousRole,
            UserRole currentRole,
            Instant occurredAt
    ) {
        this.userAccountId = Objects.requireNonNull(userAccountId, "User account id must not be null");
        this.previousRole = Objects.requireNonNull(previousRole, "Previous role must not be null");
        this.currentRole = Objects.requireNonNull(currentRole, "Current role must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred time must not be null");
    }

    public static UserAccountRoleChangedEvent of(
            UserAccountId userAccountId,
            UserRole previousRole,
            UserRole currentRole,
            Instant occurredAt
    ) {
        if (previousRole == currentRole) {
            throw new IllegalArgumentException("Role changed event requires different previous and current roles");
        }
        return new UserAccountRoleChangedEvent(userAccountId, previousRole, currentRole, occurredAt);
    }

    public UserAccountId getUserAccountId() {
        return userAccountId;
    }

    public UserRole getPreviousRole() {
        return previousRole;
    }

    public UserRole getCurrentRole() {
        return currentRole;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
