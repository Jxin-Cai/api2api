package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Model name value object.
 */
public final class ModelName {

    private static final int MAX_LENGTH = 255;

    private final String value;

    private ModelName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Model name must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Model name length must be between 1 and 255");
        }
        this.value = trimmed;
    }

    public static ModelName of(String value) {
        return new ModelName(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModelName modelName)) {
            return false;
        }
        return Objects.equals(value, modelName.value);
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
