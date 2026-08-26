package com.api2api.ohs.http.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paged channel evaluation history response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEvaluationHistoryResponse {

    private List<ChannelEvaluationResponse> evaluations;
    private ChannelEvaluationScoreSummaryResponse summary;
    private long totalElements;
    private int limit;
    private int offset;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelEvaluationScoreSummaryResponse {
        private long totalCount;
        private long scoredCount;
        private long failedCount;
        private BigDecimal averageScore;
        private BigDecimal minScore;
        private BigDecimal maxScore;
    }
}
