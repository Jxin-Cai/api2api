package com.api2api.infr.repository.usage.po;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persistence object mapped to the usage_records fact table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordPO {

    private Long id;
    private String requestId;
    private Long userAccountId;
    private Long apiCredentialId;
    private String requestedModel;
    private String upstreamModel;
    private String requestProtocol;
    private String upstreamProtocol;
    private Long providerChannelId;
    private String status;
    private long inputTokens;
    private long outputTokens;
    private long cacheCreationInputTokens;
    private long cacheReadInputTokens;
    private long totalTokens;
    private boolean usageKnown;
    private boolean streaming;
    private Instant startedTime;
    private Instant endedTime;
    private long durationMillis;
    private String errorType;
    private String errorMessage;
    private String routeFailuresJson;
    private Instant createdTime;
    private Instant updatedTime;
    private boolean deleted;
}
