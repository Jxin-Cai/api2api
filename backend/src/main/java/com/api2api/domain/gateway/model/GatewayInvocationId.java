package com.api2api.domain.gateway.model;

import java.util.Objects;

/**
 * Unique identifier of a gateway invocation.
 */
public final class GatewayInvocationId {

    private final Long value;

    private GatewayInvocationId(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("Gateway invocation id must not be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Gateway invocation id must be greater than 0");
        }
        this.value = value;
    }

    public static GatewayInvocationId of(Long value) {
        return new GatewayInvocationId(value);
    }

    public Long value() {
        return value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GatewayInvocationId that)) {
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
        return "GatewayInvocationId{" +
                "value=" + value +
                '}';
    }
}
