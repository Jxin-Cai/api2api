package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Provider channel name value object.
 */
public final class ProviderChannelName {

    private static final int MAX_LENGTH = 100;

    private final String value;

    private ProviderChannelName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Provider channel name must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Provider channel name length must be between 1 and 100");
        }
        this.value = trimmed;
    }

    public static ProviderChannelName of(String value) {
        return new ProviderChannelName(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderChannelName that)) {
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
