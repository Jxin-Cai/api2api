package com.api2api.domain.user.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing an internal user account and its authorization boundary.
 */
public final class UserAccount {

    private final UserAccountId id;
    private final Username username;
    private DisplayName displayName;
    private UserRole role;
    private UserAccountStatus status;
    private PasswordHash passwordHash;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserAccount(
            UserAccountId id,
            Username username,
            DisplayName displayName,
            UserRole role,
            UserAccountStatus status,
            PasswordHash passwordHash,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "User account id must not be null");
        this.username = Objects.requireNonNull(username, "Username must not be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name must not be null");
        this.role = Objects.requireNonNull(role, "User role must not be null");
        this.status = Objects.requireNonNull(status, "User account status must not be null");
        this.passwordHash = passwordHash;
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
    }

    public static UserAccount create(
            UserAccountId id,
            Username username,
            DisplayName displayName,
            UserRole role,
            PasswordHash passwordHash,
            Instant now
    ) {
        Objects.requireNonNull(passwordHash, "Password hash must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        return new UserAccount(id, username, displayName, role, UserAccountStatus.ACTIVE, passwordHash, now, now);
    }

    public static UserAccount rehydrate(
            UserAccountId id,
            Username username,
            DisplayName displayName,
            UserRole role,
            UserAccountStatus status,
            PasswordHash passwordHash,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new UserAccount(id, username, displayName, role, status, passwordHash, createdAt, updatedAt);
    }

    public void changeDisplayName(DisplayName displayName, Instant now) {
        Objects.requireNonNull(displayName, "Display name must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        this.displayName = displayName;
        this.updatedAt = now;
    }

    public void changeRole(UserRole newRole, Instant now) {
        Objects.requireNonNull(newRole, "New user role must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.role == newRole) {
            return;
        }
        this.role = newRole;
        this.updatedAt = now;
    }

    public void changePasswordHash(PasswordHash passwordHash, Instant now) {
        Objects.requireNonNull(passwordHash, "Password hash must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (passwordHash.equals(this.passwordHash)) {
            return;
        }
        this.passwordHash = passwordHash;
        this.updatedAt = now;
    }

    public void ensureAdminActive(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        boolean changed = false;
        if (this.role != UserRole.ADMIN) {
            this.role = UserRole.ADMIN;
            changed = true;
        }
        if (this.status != UserAccountStatus.ACTIVE) {
            this.status = UserAccountStatus.ACTIVE;
            changed = true;
        }
        if (changed) {
            this.updatedAt = now;
        }
    }

    public void disable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.status == UserAccountStatus.DISABLED) {
            return;
        }
        this.status = UserAccountStatus.DISABLED;
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.status == UserAccountStatus.ACTIVE) {
            return;
        }
        this.status = UserAccountStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void assertActive() {
        if (this.status != UserAccountStatus.ACTIVE) {
            throw new IllegalStateException("User account is disabled");
        }
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isRegularUser() {
        return this.role == UserRole.USER;
    }

    public boolean canAccess(AccessScope scope) {
        Objects.requireNonNull(scope, "Access scope must not be null");
        return this.role.grants(scope);
    }

    public void assertCanAccess(AccessScope scope) {
        assertActive();
        if (!canAccess(scope)) {
            throw new IllegalStateException("ACCESS_DENIED: user account cannot access requested scope");
        }
    }

    public boolean canViewUsageOf(UserAccountId targetUserId) {
        Objects.requireNonNull(targetUserId, "Target user account id must not be null");
        return isAdmin() || this.id.equals(targetUserId);
    }

    public boolean canViewProviderChannelInfo() {
        return isAdmin();
    }

    public UserAccountId getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public DisplayName getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public UserAccountStatus getStatus() {
        return status;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public boolean hasPasswordHash() {
        return passwordHash != null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
