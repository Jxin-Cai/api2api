package com.api2api.domain.routing.model;

import java.util.Locale;

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

    public static RouteFailureType fromHttpStatus(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return AUTHORIZATION_ERROR;
        }
        if (statusCode == 429) {
            return RATE_LIMITED;
        }
        if (statusCode >= 500) {
            return CHANNEL_UNAVAILABLE;
        }
        return UPSTREAM_ERROR;
    }

    public static boolean isModelUnavailableResponse(int statusCode, String responseBody) {
        if (statusCode != 404 || responseBody == null) {
            return false;
        }
        String normalized = responseBody.toLowerCase(Locale.ROOT);
        return normalized.contains("model_not_found")
                || normalized.contains("model not found")
                || normalized.contains("not supported by any configured account");
    }
}
