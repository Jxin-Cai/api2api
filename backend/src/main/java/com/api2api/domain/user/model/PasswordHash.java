package com.api2api.domain.user.model;

import java.util.Objects;

/**
 * Hashed password stored for a user account.
 */
public final class PasswordHash {

    private static final int MAX_LENGTH = 255;

    private final String value;

    private PasswordHash(String value) {
        String normalized = normalize(value);
        validate(normalized);
        this.value = normalized;
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    public static PasswordHash ofNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new PasswordHash(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Password hash must not be null");
        }
        return value.trim();
    }

    private static void validate(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password hash length must not exceed 255 characters");
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PasswordHash that)) {
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
        return "PasswordHash{value='[PROTECTED]'}";
    }
}
