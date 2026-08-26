package com.api2api.ohs.http.admin.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Channel evaluation run response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEvaluationResponse {

    private Long id;
    private Long providerChannelId;
    private String requestedModel;
    private String upstreamFormat;
    private String providerRunId;
    private String status;
    private String trigger;
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
    private Long requestedAt;
    private Long startedAt;
    private Long completedAt;
    private Long createdAt;
    private Long updatedAt;
}
