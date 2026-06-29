package com.api2api.ohs.http.usage.dto;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.usage.model.UsageRecordStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usage record response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordResponse {

    private Long id;
    private String requestId;
    private Long userAccountId;
    private Long apiCredentialId;
    private String requestedModel;
    private String upstreamModel;
    private ProtocolType requestProtocol;
    private ProtocolType upstreamProtocol;
    private Long providerChannelId;
    private UsageRecordStatus status;
    private long inputTokens;
    private long outputTokens;
    private long cacheCreationInputTokens;
    private long cacheReadInputTokens;
    private long totalTokens;
    private boolean usageKnown;
    private boolean streaming;
    private String errorType;
    private String errorMessage;
    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;
}
