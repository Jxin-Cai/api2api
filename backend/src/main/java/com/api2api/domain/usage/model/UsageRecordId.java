package com.api2api.domain.usage.model;

import java.util.Objects;

/**
 * Unique identifier of a usage record.
 */
public final class UsageRecordId {

    private final Long value;

    private UsageRecordId(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("Usage record id must not be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Usage record id must be greater than 0");
        }
        this.value = value;
    }

    public static UsageRecordId of(Long value) {
        return new UsageRecordId(value);
    }

    public Long value() {
        return value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageRecordId that)) {
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
        return "UsageRecordId{" +
                "value=" + value +
                '}';
    }
}
