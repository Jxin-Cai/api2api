package com.api2api.domain.analytics.model;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import java.time.Instant;
import java.util.Objects;

/**
 * Token trend bucket grouped by one API credential owned by the current user.
 */
public final class CredentialTokenTrendPoint {

    private final Instant bucketStart;
    private final Instant bucketEnd;
    private final ApiCredentialId credentialId;
    private final ApiCredentialName credentialName;
    private final TokenAmount totalTokens;

    private CredentialTokenTrendPoint(
            Instant bucketStart,
            Instant bucketEnd,
            ApiCredentialId credentialId,
            ApiCredentialName credentialName,
            TokenAmount totalTokens
    ) {
        this.bucketStart = Objects.requireNonNull(bucketStart, "Credential trend bucket start must not be null");
        this.bucketEnd = Objects.requireNonNull(bucketEnd, "Credential trend bucket end must not be null");
        if (!this.bucketEnd.isAfter(this.bucketStart)) {
            throw new IllegalArgumentException("Credential trend bucket end must be after bucket start");
        }
        this.credentialId = Objects.requireNonNull(credentialId, "Credential trend credential id must not be null");
        this.credentialName = Objects.requireNonNull(credentialName, "Credential trend credential name must not be null");
        this.totalTokens = Objects.requireNonNull(totalTokens, "Credential trend total tokens must not be null");
    }

    public static CredentialTokenTrendPoint of(
            Instant bucketStart,
            Instant bucketEnd,
            ApiCredentialId credentialId,
            ApiCredentialName credentialName,
            TokenAmount totalTokens
    ) {
        return new CredentialTokenTrendPoint(bucketStart, bucketEnd, credentialId, credentialName, totalTokens);
    }

    public static CredentialTokenTrendPoint zero(
            Instant bucketStart,
            Instant bucketEnd,
            ApiCredentialId credentialId,
            ApiCredentialName credentialName
    ) {
        return new CredentialTokenTrendPoint(bucketStart, bucketEnd, credentialId, credentialName, TokenAmount.zero());
    }

    public Instant bucketStart() {
        return bucketStart;
    }

    public Instant bucketEnd() {
        return bucketEnd;
    }

    public ApiCredentialId credentialId() {
        return credentialId;
    }

    public ApiCredentialName credentialName() {
        return credentialName;
    }

    public TokenAmount totalTokens() {
        return totalTokens;
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

    public TokenAmount getTotalTokens() {
        return totalTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CredentialTokenTrendPoint that)) {
            return false;
        }
        return Objects.equals(bucketStart, that.bucketStart)
                && Objects.equals(bucketEnd, that.bucketEnd)
                && Objects.equals(credentialId, that.credentialId)
                && Objects.equals(credentialName, that.credentialName)
                && Objects.equals(totalTokens, that.totalTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketStart, bucketEnd, credentialId, credentialName, totalTokens);
    }
}
