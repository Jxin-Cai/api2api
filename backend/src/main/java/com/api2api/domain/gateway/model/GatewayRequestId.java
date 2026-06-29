package com.api2api.domain.gateway.model;

import java.util.Objects;

/**
 * External request tracing identifier generated before entering the domain.
 */
public final class GatewayRequestId {

    private static final int MAX_LENGTH = 64;

    private final String value;

    private GatewayRequestId(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Gateway request id must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Gateway request id length must be between 1 and 64");
        }
        this.value = trimmed;
    }

    public static GatewayRequestId of(String value) {
        return new GatewayRequestId(value);
    }

    public String value() {
        return value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GatewayRequestId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
