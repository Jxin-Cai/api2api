package com.api2api.domain.analytics.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Left-closed and right-open analytics time window with display timezone.
 */
public final class AnalyticsTimeWindow {

    private final Instant startInclusive;
    private final Instant endExclusive;
    private final String zoneId;

    private AnalyticsTimeWindow(Instant startInclusive, Instant endExclusive, String zoneId) {
        this.startInclusive = Objects.requireNonNull(startInclusive, "Analytics window start time must not be null");
        this.endExclusive = Objects.requireNonNull(endExclusive, "Analytics window end time must not be null");
        if (!this.endExclusive.isAfter(this.startInclusive)) {
            throw new IllegalArgumentException("Analytics window end time must be after start time");
        }
        if (zoneId == null) {
            throw new IllegalArgumentException("Analytics window zone id must not be null");
        }
        String normalizedZoneId = zoneId.trim();
        if (normalizedZoneId.isEmpty()) {
            throw new IllegalArgumentException("Analytics window zone id must not be blank");
        }
        this.zoneId = normalizedZoneId;
    }

    public static AnalyticsTimeWindow of(Instant startInclusive, Instant endExclusive, String zoneId) {
        return new AnalyticsTimeWindow(startInclusive, endExclusive, zoneId);
    }

    public boolean contains(Instant instant) {
        Instant nonNullInstant = Objects.requireNonNull(instant, "Analytics instant must not be null");
        return !nonNullInstant.isBefore(startInclusive) && nonNullInstant.isBefore(endExclusive);
    }

    public long durationMinutes() {
        long seconds = Duration.between(startInclusive, endExclusive).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    public double durationMinutesExact() {
        return Duration.between(startInclusive, endExclusive).toMillis() / 60_000.0d;
    }

    public Instant startInclusive() {
        return startInclusive;
    }

    public Instant endExclusive() {
        return endExclusive;
    }

    public String zoneId() {
        return zoneId;
    }

    public Instant getStartInclusive() {
        return startInclusive;
    }

    public Instant getEndExclusive() {
        return endExclusive;
    }

    public String getZoneId() {
        return zoneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnalyticsTimeWindow that)) {
            return false;
        }
        return Objects.equals(startInclusive, that.startInclusive)
                && Objects.equals(endExclusive, that.endExclusive)
                && Objects.equals(zoneId, that.zoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startInclusive, endExclusive, zoneId);
    }
}
