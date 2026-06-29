package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Route priority value object. Smaller value means higher priority.
 */
public final class RoutePriority implements Comparable<RoutePriority> {

    private final int value;

    private RoutePriority(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Route priority must be greater than or equal to 1");
        }
        this.value = value;
    }

    public static RoutePriority of(int value) {
        return new RoutePriority(value);
    }

    public int value() {
        return value;
    }

    @Override
    public int compareTo(RoutePriority other) {
        Objects.requireNonNull(other, "Route priority to compare must not be null");
        return Integer.compare(this.value, other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoutePriority that)) {
            return false;
        }
        return value == that.value;
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
