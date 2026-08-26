package com.api2api.infr.repository.evaluation.po;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEvaluationPO {

    private Long id;
    private Long providerChannelId;
    private String requestedModel;
    private String upstreamFormat;
    private String providerRunId;
    private String status;
    private String triggerType;
    private BigDecimal score;
    private String detectedFamily;
    private String detectedModel;
    private BigDecimal detectedConfidence;
    private Boolean familyMismatch;
    private String channelSignature;
    private String reportUrl;
    private Integer passedProbeCount;
    private Integer warningProbeCount;
    private Integer failedProbeCount;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private String errorMessage;
    private String reportSummary;
    private Instant requestedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
