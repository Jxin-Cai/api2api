package com.api2api.domain.evaluation.model;

import java.util.Objects;

/**
 * Channel evaluation identifier value object.
 */
public final class ChannelEvaluationId {

    private final Long value;

    private ChannelEvaluationId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Channel evaluation id must be greater than 0");
        }
        this.value = value;
    }

    public static ChannelEvaluationId of(Long value) {
        return new ChannelEvaluationId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelEvaluationId that)) {
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
        return String.valueOf(value);
    }
}
