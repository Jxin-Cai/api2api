package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * Masked preview of an API key for display.
 */
public final class ApiKeyPreview {

    private static final int MAX_LENGTH = 100;

    private final String value;

    private ApiKeyPreview(String value) {
        if (value == null) {
            throw new IllegalArgumentException("API key preview must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("API key preview length must be between 1 and 100");
        }
        if (!looksMasked(trimmed)) {
            throw new IllegalArgumentException("API key preview must be a masked display fragment, not a full plaintext key");
        }
        this.value = trimmed;
    }

    public static ApiKeyPreview of(String value) {
        return new ApiKeyPreview(value);
    }

    private static boolean looksMasked(String value) {
        return value.contains("*")
                || value.contains("…")
                || value.contains("...")
                || value.length() <= 16;
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
        if (!(o instanceof ApiKeyPreview that)) {
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
