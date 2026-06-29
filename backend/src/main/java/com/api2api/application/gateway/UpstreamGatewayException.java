package com.api2api.application.gateway;

import com.api2api.domain.routing.model.RouteFailureType;
import java.util.Objects;

/**
 * Typed exception describing an upstream provider call failure, carrying a
 * classified failure type and sanitized diagnostic instead of raw upstream body.
 */
public class UpstreamGatewayException extends RuntimeException {

    private final RouteFailureType failureType;
    private final Integer statusCode;
    private final boolean retryable;
    private final long elapsedMillis;

    public UpstreamGatewayException(
            RouteFailureType failureType,
            Integer statusCode,
            boolean retryable,
            long elapsedMillis,
            String diagnostic
    ) {
        super(diagnostic);
        this.failureType = Objects.requireNonNull(failureType, "Failure type must not be null");
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.elapsedMillis = Math.max(0, elapsedMillis);
    }

    public RouteFailureType failureType() {
        return failureType;
    }

    public Integer statusCode() {
        return statusCode;
    }

    public boolean retryable() {
        return retryable;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }
}
