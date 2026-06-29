package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * Hash or fingerprint of an API key. Plaintext key material is not represented in the domain model.
 */
public final class ApiKeyHash {

    private static final int MIN_LENGTH = 32;
    private static final int MAX_LENGTH = 255;

    private final String value;

    private ApiKeyHash(String value) {
        if (value == null) {
            throw new IllegalArgumentException("API key hash must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("API key hash length must be between 32 and 255");
        }
        this.value = trimmed;
    }

    public static ApiKeyHash of(String value) {
        return new ApiKeyHash(value);
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
        if (!(o instanceof ApiKeyHash that)) {
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
        return "ApiKeyHash{masked}";
    }
}
