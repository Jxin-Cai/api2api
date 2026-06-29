package com.api2api.ohs.http.gateway;

import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Raw protocol response returned to external SDKs without management API wrapping.
 */
public final class GatewayRawResponse {

    private final String body;
    private final HttpStatus status;
    private final MediaType contentType;

    private GatewayRawResponse(String body, HttpStatus status, MediaType contentType) {
        this.body = Objects.requireNonNull(body, "Response body must not be null");
        this.status = Objects.requireNonNull(status, "HTTP status must not be null");
        this.contentType = Objects.requireNonNull(contentType, "Content type must not be null");
    }

    public static GatewayRawResponse of(String body, HttpStatus status, MediaType contentType) {
        return new GatewayRawResponse(body, status, contentType);
    }

    public ResponseEntity<String> toResponseEntity() {
        return ResponseEntity.status(status)
                .contentType(contentType)
                .body(body);
    }

    public String body() {
        return body;
    }

    public HttpStatus status() {
        return status;
    }

    public MediaType contentType() {
        return contentType;
    }
}
