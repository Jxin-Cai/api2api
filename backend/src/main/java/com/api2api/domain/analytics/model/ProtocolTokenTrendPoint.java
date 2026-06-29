package com.api2api.domain.analytics.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.time.Instant;
import java.util.Objects;

/**
 * Token trend bucket for one request protocol.
 */
public final class ProtocolTokenTrendPoint {

    private final Instant bucketStart;
    private final Instant bucketEnd;
    private final ProtocolType protocol;
    private final TokenAmount totalTokens;

    private ProtocolTokenTrendPoint(
            Instant bucketStart,
            Instant bucketEnd,
            ProtocolType protocol,
            TokenAmount totalTokens
    ) {
        this.bucketStart = Objects.requireNonNull(bucketStart, "Protocol trend bucket start must not be null");
        this.bucketEnd = Objects.requireNonNull(bucketEnd, "Protocol trend bucket end must not be null");
        if (!this.bucketEnd.isAfter(this.bucketStart)) {
            throw new IllegalArgumentException("Protocol trend bucket end must be after bucket start");
        }
        this.protocol = Objects.requireNonNull(protocol, "Protocol trend protocol must not be null");
        this.totalTokens = Objects.requireNonNull(totalTokens, "Protocol trend total tokens must not be null");
    }

    public static ProtocolTokenTrendPoint of(
            Instant bucketStart,
            Instant bucketEnd,
            ProtocolType protocol,
            TokenAmount totalTokens
    ) {
        return new ProtocolTokenTrendPoint(bucketStart, bucketEnd, protocol, totalTokens);
    }

    public static ProtocolTokenTrendPoint zero(Instant bucketStart, Instant bucketEnd, ProtocolType protocol) {
        return new ProtocolTokenTrendPoint(bucketStart, bucketEnd, protocol, TokenAmount.zero());
    }

    public Instant bucketStart() {
        return bucketStart;
    }

    public Instant bucketEnd() {
        return bucketEnd;
    }

    public ProtocolType protocol() {
        return protocol;
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

    public ProtocolType getProtocol() {
        return protocol;
    }

    public TokenAmount getTotalTokens() {
        return totalTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolTokenTrendPoint that)) {
            return false;
        }
        return Objects.equals(bucketStart, that.bucketStart)
                && Objects.equals(bucketEnd, that.bucketEnd)
                && protocol == that.protocol
                && Objects.equals(totalTokens, that.totalTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketStart, bucketEnd, protocol, totalTokens);
    }
}
