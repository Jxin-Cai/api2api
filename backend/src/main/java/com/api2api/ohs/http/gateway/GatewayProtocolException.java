package com.api2api.ohs.http.gateway;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;
import org.springframework.http.HttpStatus;

/**
 * Protocol-shaped error raised by gateway endpoints so SDK clients receive
 * Claude/OpenAI compatible error bodies instead of management API responses.
 */
public class GatewayProtocolException extends RuntimeException {

    private final ProtocolType protocol;
    private final HttpStatus status;
    private final String errorType;

    public GatewayProtocolException(ProtocolType protocol, HttpStatus status, String errorType, String message) {
        super(message);
        this.protocol = Objects.requireNonNull(protocol, "Protocol must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.errorType = Objects.requireNonNull(errorType, "Error type must not be null");
    }

    public static GatewayProtocolException badRequest(ProtocolType protocol, String message) {
        return new GatewayProtocolException(protocol, HttpStatus.BAD_REQUEST, "invalid_request_error", message);
    }

    public ProtocolType protocol() {
        return protocol;
    }

    public HttpStatus status() {
        return status;
    }

    public String errorType() {
        return errorType;
    }
}
