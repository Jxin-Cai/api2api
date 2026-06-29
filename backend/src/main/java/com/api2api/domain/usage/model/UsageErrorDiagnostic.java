package com.api2api.domain.usage.model;

import com.api2api.domain.gateway.model.InvocationError;
import com.api2api.domain.gateway.model.InvocationErrorType;
import com.api2api.domain.routing.model.RouteFailure;
import java.util.List;
import java.util.Objects;

/**
 * Failure diagnostic captured by a failed usage record.
 */
public final class UsageErrorDiagnostic {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final InvocationErrorType errorType;
    private final String message;
    private final List<RouteFailure> routeFailures;

    private UsageErrorDiagnostic(InvocationErrorType errorType, String message, List<RouteFailure> routeFailures) {
        this.errorType = Objects.requireNonNull(errorType, "Invocation error type must not be null");
        this.message = normalizeMessage(message);
        Objects.requireNonNull(routeFailures, "Route failures must not be null");
        this.routeFailures = routeFailures.stream()
                .map(routeFailure -> Objects.requireNonNull(routeFailure, "Route failure must not be null"))
                .toList();
    }

    public static UsageErrorDiagnostic of(InvocationErrorType errorType, String message, List<RouteFailure> routeFailures) {
        return new UsageErrorDiagnostic(errorType, message, routeFailures);
    }

    public static UsageErrorDiagnostic fromInvocationError(InvocationError error) {
        InvocationError nonNullError = Objects.requireNonNull(error, "Invocation error must not be null");
        return new UsageErrorDiagnostic(nonNullError.errorType(), nonNullError.message(), nonNullError.failures());
    }

    public UsageErrorDiagnostic redactChannelDetails() {
        return new UsageErrorDiagnostic(errorType, message, List.of());
    }

    private static String normalizeMessage(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Usage error message must not be null");
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Usage error message length must be between 1 and 1000");
        }
        return trimmed;
    }

    public InvocationErrorType errorType() {
        return errorType;
    }

    public String message() {
        return message;
    }

    public List<RouteFailure> routeFailures() {
        return List.copyOf(routeFailures);
    }

    public InvocationErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public List<RouteFailure> getRouteFailures() {
        return routeFailures();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageErrorDiagnostic that)) {
            return false;
        }
        return errorType == that.errorType
                && Objects.equals(message, that.message)
                && Objects.equals(routeFailures, that.routeFailures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorType, message, routeFailures);
    }
}
