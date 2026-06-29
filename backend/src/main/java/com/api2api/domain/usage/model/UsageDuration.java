package com.api2api.domain.usage.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Non-negative duration of a gateway invocation measured in milliseconds.
 */
public final class UsageDuration {

    private final long millis;

    private UsageDuration(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Usage duration millis must be greater than or equal to 0");
        }
        this.millis = millis;
    }

    public static UsageDuration of(long millis) {
        return new UsageDuration(millis);
    }

    public static UsageDuration between(Instant startedAt, Instant endedAt) {
        Instant nonNullStartedAt = Objects.requireNonNull(startedAt, "Started time must not be null");
        Instant nonNullEndedAt = Objects.requireNonNull(endedAt, "Ended time must not be null");
        if (nonNullEndedAt.isBefore(nonNullStartedAt)) {
            throw new IllegalArgumentException("Ended time must not be before started time");
        }
        return new UsageDuration(Duration.between(nonNullStartedAt, nonNullEndedAt).toMillis());
    }

    public long millis() {
        return millis;
    }

    public long getMillis() {
        return millis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageDuration that)) {
            return false;
        }
        return millis == that.millis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(millis);
    }

    @Override
    public String toString() {
        return "UsageDuration{" +
                "millis=" + millis +
                '}';
    }
}
