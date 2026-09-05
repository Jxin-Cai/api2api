package com.api2api.domain.analytics.model;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import java.util.Objects;

/**
 * Provider channel ranked by its slowest single response inside a time window.
 */
public record ChannelLatencyRanking(
        int rank,
        ProviderChannelId providerChannelId,
        ProviderChannelName providerChannelName,
        long maxDurationMillis,
        long avgDurationMillis,
        long maxFirstTokenMillis,
        long avgFirstTokenMillis,
        long requestCount
) {

    public ChannelLatencyRanking {
        if (rank < 1) {
            throw new IllegalArgumentException("Channel latency rank must be at least 1");
        }
        Objects.requireNonNull(providerChannelId, "Channel latency provider channel id must not be null");
        Objects.requireNonNull(providerChannelName, "Channel latency provider channel name must not be null");
        if (maxDurationMillis < 0 || avgDurationMillis < 0) {
            throw new IllegalArgumentException("Channel latency durations must not be negative");
        }
        if (requestCount < 1) {
            throw new IllegalArgumentException("Channel latency request count must be at least 1");
        }
    }

    public int getRank() {
        return rank;
    }

    public ProviderChannelId getProviderChannelId() {
        return providerChannelId;
    }

    public ProviderChannelName getProviderChannelName() {
        return providerChannelName;
    }

    public long getMaxDurationMillis() {
        return maxDurationMillis;
    }

    public long getAvgDurationMillis() {
        return avgDurationMillis;
    }

    public long getMaxFirstTokenMillis() { return maxFirstTokenMillis; }

    public long getAvgFirstTokenMillis() { return avgFirstTokenMillis; }

    public long getRequestCount() {
        return requestCount;
    }
}
