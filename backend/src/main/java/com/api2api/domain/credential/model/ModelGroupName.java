package com.api2api.domain.credential.model;

import java.util.Objects;

/** User-visible name of a model access group. */
public final class ModelGroupName {

    private static final int MAX_LENGTH = 100;
    private final String value;

    private ModelGroupName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Model group name must not be null");
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Model group name length must be between 1 and 100");
        }
        this.value = normalized;
    }

    public static ModelGroupName of(String value) {
        return new ModelGroupName(value);
    }

    public String value() {
        return value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ModelGroupName that && Objects.equals(value, that.value);
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
