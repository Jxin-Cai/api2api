package com.api2api.domain.user.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Login name of a user account.
 */
public final class Username {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 64;
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]+$");

    private final String value;

    private Username(String value) {
        String normalized = normalize(value);
        validate(normalized);
        this.value = normalized;
    }

    public static Username of(String value) {
        return new Username(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Username must not be null");
        }
        return value.trim();
    }

    private static void validate(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Username length must be between 3 and 64 characters");
        }
        if (!ALLOWED_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Username may contain only letters, numbers, underscores, hyphens and dots");
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
        if (!(o instanceof Username username)) {
            return false;
        }
        return Objects.equals(value, username.value);
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
