package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Secret reference value object. It carries only a reference, never plaintext secret.
 */
public final class SecretRef {

    private static final int MAX_LENGTH = 255;

    private final String value;

    private SecretRef(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Secret reference must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Secret reference length must be between 1 and 255");
        }
        this.value = trimmed;
    }

    public static SecretRef of(String value) {
        return new SecretRef(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecretRef secretRef)) {
            return false;
        }
        return Objects.equals(value, secretRef.value);
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
