package com.api2api.domain.analytics.model;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import java.time.Instant;
import java.util.Objects;

/**
 * Peak in-flight requests of one API credential inside one time bucket.
 */
public record CredentialConcurrencyTrendPoint(
        Instant bucketStart,
        Instant bucketEnd,
        ApiCredentialId credentialId,
        ApiCredentialName credentialName,
        int peakConcurrency
) {

    public CredentialConcurrencyTrendPoint {
        Objects.requireNonNull(bucketStart, "Credential concurrency bucket start must not be null");
        Objects.requireNonNull(bucketEnd, "Credential concurrency bucket end must not be null");
        if (!bucketEnd.isAfter(bucketStart)) {
            throw new IllegalArgumentException("Credential concurrency bucket end must be after bucket start");
        }
        Objects.requireNonNull(credentialId, "Credential concurrency credential id must not be null");
        Objects.requireNonNull(credentialName, "Credential concurrency credential name must not be null");
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

    public ApiCredentialId getCredentialId() {
        return credentialId;
    }

    public ApiCredentialName getCredentialName() {
        return credentialName;
    }

    public int getPeakConcurrency() {
        return peakConcurrency;
    }
}
