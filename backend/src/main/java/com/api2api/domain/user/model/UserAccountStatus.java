package com.api2api.domain.user.model;

/**
 * Availability status of a user account.
 */
public enum UserAccountStatus {
    /**
     * Account is enabled and may pass active checks.
     */
    ACTIVE,

    /**
     * Account is disabled and must be rejected by active checks.
     */
    DISABLED
}
