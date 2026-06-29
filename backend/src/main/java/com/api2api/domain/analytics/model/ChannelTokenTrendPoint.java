package com.api2api.domain.analytics.model;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import java.time.Instant;
import java.util.Objects;

/**
 * Administrative token trend bucket grouped by provider channel.
 */
public final class ChannelTokenTrendPoint {

    private final Instant bucketStart;
    private final Instant bucketEnd;
    private final ProviderChannelId providerChannelId;
    private final ProviderChannelName providerChannelName;
    private final TokenAmount totalTokens;

    private ChannelTokenTrendPoint(
            Instant bucketStart,
            Instant bucketEnd,
            ProviderChannelId providerChannelId,
            ProviderChannelName providerChannelName,
            TokenAmount totalTokens
    ) {
        this.bucketStart = Objects.requireNonNull(bucketStart, "Channel trend bucket start must not be null");
        this.bucketEnd = Objects.requireNonNull(bucketEnd, "Channel trend bucket end must not be null");
        if (!this.bucketEnd.isAfter(this.bucketStart)) {
            throw new IllegalArgumentException("Channel trend bucket end must be after bucket start");
        }
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Channel trend provider channel id must not be null");
        this.providerChannelName = Objects.requireNonNull(providerChannelName, "Channel trend provider channel name must not be null");
        this.totalTokens = Objects.requireNonNull(totalTokens, "Channel trend total tokens must not be null");
    }

    public static ChannelTokenTrendPoint of(
            Instant bucketStart,
            Instant bucketEnd,
            ProviderChannelId providerChannelId,
            ProviderChannelName providerChannelName,
            TokenAmount totalTokens
    ) {
        return new ChannelTokenTrendPoint(bucketStart, bucketEnd, providerChannelId, providerChannelName, totalTokens);
    }

    public Instant bucketStart() {
        return bucketStart;
    }

    public Instant bucketEnd() {
        return bucketEnd;
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public ProviderChannelName providerChannelName() {
        return providerChannelName;
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

    public ProviderChannelId getProviderChannelId() {
        return providerChannelId;
    }

    public ProviderChannelName getProviderChannelName() {
        return providerChannelName;
    }

    public TokenAmount getTotalTokens() {
        return totalTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelTokenTrendPoint that)) {
            return false;
        }
        return Objects.equals(bucketStart, that.bucketStart)
                && Objects.equals(bucketEnd, that.bucketEnd)
                && Objects.equals(providerChannelId, that.providerChannelId)
                && Objects.equals(providerChannelName, that.providerChannelName)
                && Objects.equals(totalTokens, that.totalTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketStart, bucketEnd, providerChannelId, providerChannelName, totalTokens);
    }
}
