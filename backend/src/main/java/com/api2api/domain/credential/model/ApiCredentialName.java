package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * User visible name of an API credential.
 */
public final class ApiCredentialName {

    private static final int MAX_LENGTH = 100;

    private final String value;

    private ApiCredentialName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("API credential name must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("API credential name length must be between 1 and 100");
        }
        this.value = trimmed;
    }

    public static ApiCredentialName of(String value) {
        return new ApiCredentialName(value);
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
        if (!(o instanceof ApiCredentialName that)) {
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
