package com.api2api.domain.gateway.model;

import com.api2api.domain.routing.model.RouteFailure;
import java.util.List;
import java.util.Objects;

/**
 * Failure details for a gateway invocation.
 */
public final class InvocationError {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final InvocationErrorType errorType;
    private final String message;
    private final List<RouteFailure> failures;

    private InvocationError(InvocationErrorType errorType, String message, List<RouteFailure> failures) {
        this.errorType = Objects.requireNonNull(errorType, "Invocation error type must not be null");
        this.message = normalizeMessage(message);
        Objects.requireNonNull(failures, "Route failures must not be null");
        this.failures = List.copyOf(failures);
    }

    public static InvocationError of(InvocationErrorType errorType, String message, List<RouteFailure> failures) {
        return new InvocationError(errorType, message, failures);
    }

    public static InvocationError withoutFailures(InvocationErrorType errorType, String message) {
        return new InvocationError(errorType, message, List.of());
    }

    private static String normalizeMessage(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Invocation error message must not be null");
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Invocation error message length must be between 1 and 1000");
        }
        return trimmed;
    }

    public InvocationErrorType errorType() {
        return errorType;
    }

    public String message() {
        return message;
    }

    public List<RouteFailure> failures() {
        return failures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvocationError that)) {
            return false;
        }
        return errorType == that.errorType
                && Objects.equals(message, that.message)
                && Objects.equals(failures, that.failures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorType, message, failures);
    }
}
