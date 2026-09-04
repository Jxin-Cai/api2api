package com.api2api.infr.repository.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Half-open time bucket {@code [start, end)} used to aggregate analytics rows.
 */
record TimeBucket(Instant start, Instant end) {

    TimeBucket {
        Objects.requireNonNull(start, "Bucket start must not be null");
        Objects.requireNonNull(end, "Bucket end must not be null");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Bucket end must be after bucket start");
        }
    }

    boolean contains(Instant instant) {
        return !instant.isBefore(start) && instant.isBefore(end);
    }

    /**
     * Splits {@code [windowStart, windowEnd)} into consecutive buckets of {@code size}; the last bucket is clipped
     * to the window end. When {@code cutoff} is non-null, buckets that start after it are dropped so a curve never
     * extends into the future.
     */
    static List<TimeBucket> split(Instant windowStart, Instant windowEnd, Duration size, Instant cutoff) {
        Objects.requireNonNull(size, "Bucket size must not be null");
        if (size.isZero() || size.isNegative()) {
            throw new IllegalArgumentException("Bucket size must be positive");
        }
        List<TimeBucket> buckets = new ArrayList<>();
        Instant start = windowStart;
        while (start.isBefore(windowEnd) && (cutoff == null || !start.isAfter(cutoff))) {
            Instant next = start.plus(size);
            Instant end = next.isAfter(windowEnd) ? windowEnd : next;
            buckets.add(new TimeBucket(start, end));
            start = end;
        }
        return buckets;
    }
}
