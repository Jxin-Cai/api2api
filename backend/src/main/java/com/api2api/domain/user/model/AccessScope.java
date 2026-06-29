package com.api2api.domain.user.model;

/**
 * Business access scope granted to user accounts by role.
 */
public enum AccessScope {
    /**
     * User portal for personal dashboard, API keys and personal usage records.
     */
    USER_PORTAL,

    /**
     * Administrative back-office for users, channels, protocols and platform-wide analytics.
     */
    ADMIN_BACKOFFICE
}
