package com.api2api.domain.analytics.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Peak number of simultaneously in-flight gateway requests observed inside one time bucket.
 */
public record ConcurrencyTrendPoint(Instant bucketStart, Instant bucketEnd, int peakConcurrency) {

    public ConcurrencyTrendPoint {
        Objects.requireNonNull(bucketStart, "Concurrency bucket start must not be null");
        Objects.requireNonNull(bucketEnd, "Concurrency bucket end must not be null");
        if (!bucketEnd.isAfter(bucketStart)) {
            throw new IllegalArgumentException("Concurrency bucket end must be after bucket start");
        }
        if (peakConcurrency < 0) {
            throw new IllegalArgumentException("Peak concurrency must not be negative");
        }
    }

    public Instant getBucketStart() {
        return bucketStart;
    }

    public Instant getBucketEnd() {
        return bucketEnd;
    }

    public int getPeakConcurrency() {
        return peakConcurrency;
    }
}
