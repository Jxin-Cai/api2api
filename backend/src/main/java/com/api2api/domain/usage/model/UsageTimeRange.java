package com.api2api.domain.usage.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Left-closed and right-open usage query time range.
 */
public final class UsageTimeRange {

    private final Instant startInclusive;
    private final Instant endExclusive;

    private UsageTimeRange(Instant startInclusive, Instant endExclusive) {
        this.startInclusive = Objects.requireNonNull(startInclusive, "Start time must not be null");
        this.endExclusive = Objects.requireNonNull(endExclusive, "End time must not be null");
        if (!this.endExclusive.isAfter(this.startInclusive)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    public static UsageTimeRange of(Instant startInclusive, Instant endExclusive) {
        return new UsageTimeRange(startInclusive, endExclusive);
    }

    public boolean contains(Instant instant) {
        Instant nonNullInstant = Objects.requireNonNull(instant, "Time must not be null");
        return !nonNullInstant.isBefore(startInclusive) && nonNullInstant.isBefore(endExclusive);
    }

    public Instant startInclusive() {
        return startInclusive;
    }

    public Instant endExclusive() {
        return endExclusive;
    }

    public Instant getStartInclusive() {
        return startInclusive;
    }

    public Instant getEndExclusive() {
        return endExclusive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageTimeRange that)) {
            return false;
        }
        return Objects.equals(startInclusive, that.startInclusive)
                && Objects.equals(endExclusive, that.endExclusive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startInclusive, endExclusive);
    }
}
