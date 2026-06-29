package com.api2api.domain.user.model;

import java.util.Objects;

/**
 * Role of a user account.
 */
public enum UserRole {
    /**
     * Back-office administrator.
     */
    ADMIN,

    /**
     * Regular portal user.
     */
    USER;

    public boolean grants(AccessScope scope) {
        Objects.requireNonNull(scope, "Access scope must not be null");
        return switch (this) {
            case ADMIN -> scope == AccessScope.ADMIN_BACKOFFICE || scope == AccessScope.USER_PORTAL;
            case USER -> scope == AccessScope.USER_PORTAL;
        };
    }
}
