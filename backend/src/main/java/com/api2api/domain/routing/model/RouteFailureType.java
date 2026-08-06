package com.api2api.domain.routing.model;

import java.util.List;
import java.util.Locale;

/**
 * Route failure categories used by routing failover policy.
 */
public enum RouteFailureType {
    UPSTREAM_ERROR(List.of()),
    TIMEOUT(List.of("TIMEOUT", "TIMED_OUT")),
    RATE_LIMITED(List.of("RATE_LIMIT", "RATE LIMITED", "TOO_MANY_REQUESTS")),
    CHANNEL_UNAVAILABLE(List.of("UNAVAILABLE", "CHANNEL_UNAVAILABLE", "SERVICE_UNAVAILABLE")),
    CONVERSION_ERROR(List.of()),
    AUTHORIZATION_ERROR(List.of());

    private final List<String> messageKeywords;

    RouteFailureType(List<String> messageKeywords) {
        this.messageKeywords = messageKeywords;
    }

    public static RouteFailureType fromExceptionMessage(String message) {
        if (message == null || message.isBlank()) {
            return UPSTREAM_ERROR;
        }
        String upper = message.toUpperCase(Locale.ROOT);
        for (RouteFailureType type : values()) {
            if (!type.messageKeywords.isEmpty()
                    && type.messageKeywords.stream().anyMatch(upper::contains)) {
                return type;
            }
        }
        return UPSTREAM_ERROR;
    }

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
