package com.api2api.domain.user.model;

import java.util.Objects;

/**
 * Display name shown in back-office and user portal.
 */
public final class DisplayName {

    private static final int MAX_LENGTH = 100;

    private final String value;

    private DisplayName(String value) {
        String normalized = normalize(value);
        validate(normalized);
        this.value = normalized;
    }

    public static DisplayName of(String value) {
        return new DisplayName(value);
    }

    public DisplayName renameTo(String value) {
        return new DisplayName(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Display name must not be null");
        }
        return value.trim();
    }

    private static void validate(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Display name length must not exceed 100 characters");
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
        if (!(o instanceof DisplayName that)) {
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
