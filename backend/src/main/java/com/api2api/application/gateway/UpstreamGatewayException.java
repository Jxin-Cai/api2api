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
    private final UpstreamResponseMetadata responseMetadata;

    public UpstreamGatewayException(
            RouteFailureType failureType,
            Integer statusCode,
            boolean retryable,
            long elapsedMillis,
            String diagnostic
    ) {
        this(failureType, statusCode, retryable, elapsedMillis, diagnostic, UpstreamResponseMetadata.empty());
    }

    public UpstreamGatewayException(
            RouteFailureType failureType,
            Integer statusCode,
            boolean retryable,
            long elapsedMillis,
            String diagnostic,
            UpstreamResponseMetadata responseMetadata
    ) {
        super(diagnostic);
        this.failureType = Objects.requireNonNull(failureType, "Failure type must not be null");
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.elapsedMillis = Math.max(0, elapsedMillis);
        this.responseMetadata = responseMetadata == null ? UpstreamResponseMetadata.empty() : responseMetadata;
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

    public UpstreamResponseMetadata responseMetadata() {
        return responseMetadata;
    }
}
