package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * Token usage increase recorded for one upstream invocation.
 */
public final class TokenUsageDelta {

    private final long totalTokens;
    private final boolean usageKnown;

    private TokenUsageDelta(long totalTokens, boolean usageKnown) {
        if (totalTokens < 0) {
            throw new IllegalArgumentException("Token usage delta must not be negative");
        }
        this.totalTokens = totalTokens;
        this.usageKnown = usageKnown;
    }

    public static TokenUsageDelta of(long totalTokens, boolean usageKnown) {
        return new TokenUsageDelta(totalTokens, usageKnown);
    }

    public static TokenUsageDelta known(long totalTokens) {
        return new TokenUsageDelta(totalTokens, true);
    }

    public static TokenUsageDelta unknown() {
        return new TokenUsageDelta(0, false);
    }

    public long totalTokens() {
        return totalTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public boolean usageKnown() {
        return usageKnown;
    }

    public boolean isUsageKnown() {
        return usageKnown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenUsageDelta that)) {
            return false;
        }
        return totalTokens == that.totalTokens && usageKnown == that.usageKnown;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalTokens, usageKnown);
    }

    @Override
    public String toString() {
        return "TokenUsageDelta{" +
                "totalTokens=" + totalTokens +
                ", usageKnown=" + usageKnown +
                '}';
    }
}
