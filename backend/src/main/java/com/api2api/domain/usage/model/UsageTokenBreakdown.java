package com.api2api.domain.usage.model;

import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import java.util.Objects;

/**
 * Immutable token usage breakdown captured from an upstream invocation result.
 */
public final class UsageTokenBreakdown {

    private final long inputTokens;
    private final long outputTokens;
    private final long cacheCreationInputTokens;
    private final long cacheReadInputTokens;
    private final long totalTokens;
    private final boolean usageKnown;

    private UsageTokenBreakdown(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens,
            long totalTokens,
            boolean usageKnown
    ) {
        validateNonNegative(inputTokens, "inputTokens");
        validateNonNegative(outputTokens, "outputTokens");
        validateNonNegative(cacheCreationInputTokens, "cacheCreationInputTokens");
        validateNonNegative(cacheReadInputTokens, "cacheReadInputTokens");
        validateNonNegative(totalTokens, "totalTokens");
        long calculatedTotal = inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens;
        if (totalTokens != calculatedTotal) {
            throw new IllegalArgumentException("Total tokens must equal the sum of input, output and cache tokens");
        }
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cacheCreationInputTokens = cacheCreationInputTokens;
        this.cacheReadInputTokens = cacheReadInputTokens;
        this.totalTokens = totalTokens;
        this.usageKnown = usageKnown;
    }

    public static UsageTokenBreakdown of(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens,
            long totalTokens,
            boolean usageKnown
    ) {
        return new UsageTokenBreakdown(
                inputTokens,
                outputTokens,
                cacheCreationInputTokens,
                cacheReadInputTokens,
                totalTokens,
                usageKnown
        );
    }

    public static UsageTokenBreakdown known(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens
    ) {
        long totalTokens = inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens;
        return new UsageTokenBreakdown(
                inputTokens,
                outputTokens,
                cacheCreationInputTokens,
                cacheReadInputTokens,
                totalTokens,
                true
        );
    }

    public static UsageTokenBreakdown unknown() {
        return new UsageTokenBreakdown(0, 0, 0, 0, 0, false);
    }

    public static UsageTokenBreakdown zeroKnown() {
        return new UsageTokenBreakdown(0, 0, 0, 0, 0, true);
    }

    public static UsageTokenBreakdown fromUnified(UnifiedTokenUsage usage) {
        if (usage == null) {
            return unknown();
        }
        return new UsageTokenBreakdown(
                usage.inputTokens(),
                usage.outputTokens(),
                usage.cacheCreationInputTokens(),
                usage.cacheReadInputTokens(),
                usage.totalTokens(),
                usage.usageKnown()
        );
    }

    public UsageTokenBreakdown plus(UsageTokenBreakdown other) {
        UsageTokenBreakdown nonNullOther = Objects.requireNonNull(other, "Other token breakdown must not be null");
        return new UsageTokenBreakdown(
                inputTokens + nonNullOther.inputTokens,
                outputTokens + nonNullOther.outputTokens,
                cacheCreationInputTokens + nonNullOther.cacheCreationInputTokens,
                cacheReadInputTokens + nonNullOther.cacheReadInputTokens,
                totalTokens + nonNullOther.totalTokens,
                usageKnown && nonNullOther.usageKnown
        );
    }

    private static void validateNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }

    public long inputTokens() {
        return inputTokens;
    }

    public long outputTokens() {
        return outputTokens;
    }

    public long cacheCreationInputTokens() {
        return cacheCreationInputTokens;
    }

    public long cacheReadInputTokens() {
        return cacheReadInputTokens;
    }

    public long totalTokens() {
        return totalTokens;
    }

    public boolean usageKnown() {
        return usageKnown;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public long getCacheCreationInputTokens() {
        return cacheCreationInputTokens;
    }

    public long getCacheReadInputTokens() {
        return cacheReadInputTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public boolean isUsageKnown() {
        return usageKnown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageTokenBreakdown that)) {
            return false;
        }
        return inputTokens == that.inputTokens
                && outputTokens == that.outputTokens
                && cacheCreationInputTokens == that.cacheCreationInputTokens
                && cacheReadInputTokens == that.cacheReadInputTokens
                && totalTokens == that.totalTokens
                && usageKnown == that.usageKnown;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                inputTokens,
                outputTokens,
                cacheCreationInputTokens,
                cacheReadInputTokens,
                totalTokens,
                usageKnown
        );
    }
}
