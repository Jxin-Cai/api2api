package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Channel model support identifier value object.
 */
public final class ChannelModelSupportId {

    private final Long value;

    private ChannelModelSupportId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Channel model support id must be greater than 0");
        }
        this.value = value;
    }

    public static ChannelModelSupportId of(Long value) {
        return new ChannelModelSupportId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelModelSupportId that)) {
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
