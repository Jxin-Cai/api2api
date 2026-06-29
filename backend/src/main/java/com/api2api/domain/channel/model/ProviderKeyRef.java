package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Provider key reference value object. It carries only a reference, never plaintext secret.
 */
public final class ProviderKeyRef {

    private static final int MAX_LENGTH = 255;

    private final String value;

    private ProviderKeyRef(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Provider key reference must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Provider key reference length must be between 1 and 255");
        }
        this.value = trimmed;
    }

    public static ProviderKeyRef of(String value) {
        return new ProviderKeyRef(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderKeyRef that)) {
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
        return "***";
    }
}
