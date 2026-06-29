package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Provider channel identifier value object.
 */
public final class ProviderChannelId {

    private final Long value;

    private ProviderChannelId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Provider channel id must be greater than 0");
        }
        this.value = value;
    }

    public static ProviderChannelId of(Long value) {
        return new ProviderChannelId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderChannelId that)) {
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
