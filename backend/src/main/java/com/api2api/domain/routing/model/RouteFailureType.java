package com.api2api.domain.routing.model;

/**
 * Route failure categories used by routing failover policy.
 */
public enum RouteFailureType {
    UPSTREAM_ERROR,
    TIMEOUT,
    RATE_LIMITED,
    CHANNEL_UNAVAILABLE,
    CONVERSION_ERROR,
    AUTHORIZATION_ERROR;

    public boolean isRetryableByDefault() {
        return switch (this) {
            case UPSTREAM_ERROR, TIMEOUT, RATE_LIMITED, CHANNEL_UNAVAILABLE -> true;
            case CONVERSION_ERROR, AUTHORIZATION_ERROR -> false;
        };
    }
}
