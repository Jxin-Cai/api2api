package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * Unique identifier of an API credential.
 */
public final class ApiCredentialId {

    private final Long value;

    private ApiCredentialId(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("API credential id must not be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("API credential id must be greater than 0");
        }
        this.value = value;
    }

    public static ApiCredentialId of(Long value) {
        return new ApiCredentialId(value);
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
        if (!(o instanceof ApiCredentialId that)) {
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
        return "ApiCredentialId{" +
                "value=" + value +
                '}';
    }
}
