package com.api2api.domain.evaluation.model;

import java.util.Objects;

/**
 * Channel evaluation schedule identifier value object.
 */
public final class ChannelEvaluationScheduleId {

    private final Long value;

    private ChannelEvaluationScheduleId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Channel evaluation schedule id must be greater than 0");
        }
        this.value = value;
    }

    public static ChannelEvaluationScheduleId of(Long value) {
        return new ChannelEvaluationScheduleId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelEvaluationScheduleId that)) {
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
