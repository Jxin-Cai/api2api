package com.api2api.infr.repository.evaluation.po;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationScoreSummaryPO {

    private long totalCount;
    private long scoredCount;
    private long failedCount;
    private BigDecimal averageScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
}
