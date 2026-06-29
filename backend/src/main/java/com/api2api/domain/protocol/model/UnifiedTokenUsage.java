package com.api2api.domain.protocol.model;

/**
 * 跨协议统一 token 用量口径。
 */
public final class UnifiedTokenUsage {
    private final long inputTokens;
    private final long outputTokens;
    private final long cacheCreationInputTokens;
    private final long cacheReadInputTokens;
    private final long totalTokens;
    private final boolean usageKnown;

    private UnifiedTokenUsage(
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
        if (usageKnown && totalTokens != calculatedTotal) {
            throw new ProtocolConversionException("totalTokens must equal the sum of input, output and cache tokens");
        }
        if (!usageKnown && totalTokens != 0) {
            throw new ProtocolConversionException("unknown usage must use totalTokens 0");
        }
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cacheCreationInputTokens = cacheCreationInputTokens;
        this.cacheReadInputTokens = cacheReadInputTokens;
        this.totalTokens = totalTokens;
        this.usageKnown = usageKnown;
    }

    public static UnifiedTokenUsage known(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens
    ) {
        long total = inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens;
        return new UnifiedTokenUsage(inputTokens, outputTokens, cacheCreationInputTokens, cacheReadInputTokens, total, true);
    }

    public static UnifiedTokenUsage unknown() {
        return new UnifiedTokenUsage(0, 0, 0, 0, 0, false);
    }

    public static UnifiedTokenUsage of(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens,
            long totalTokens,
            boolean usageKnown
    ) {
        return new UnifiedTokenUsage(inputTokens, outputTokens, cacheCreationInputTokens, cacheReadInputTokens, totalTokens, usageKnown);
    }

    private static void validateNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new ProtocolConversionException(fieldName + " must be greater than or equal to 0");
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
}
