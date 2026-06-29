package com.api2api.infr.repository.usage.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageTokenSummaryPO {
    private long inputTokens;
    private long outputTokens;
    private long cacheCreationInputTokens;
    private long cacheReadInputTokens;
    private long totalTokens;
    @Builder.Default
    private boolean usageKnown = true;
}
