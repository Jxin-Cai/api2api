package com.api2api.domain.credential.model;

import java.util.Objects;

/** Unique identifier of a user-owned model access group. */
public final class ModelGroupId {

    private final Long value;

    private ModelGroupId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Model group id must be greater than 0");
        }
        this.value = value;
    }

    public static ModelGroupId of(Long value) {
        return new ModelGroupId(value);
    }

    public Long value() {
        return value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ModelGroupId that && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
