package com.api2api.domain.evaluation.repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Aggregated evaluation statistics over a time window.
 *
 * <p>Averages, minimums and maximums are computed from normalized 0-100 scores and only consider
 * successful runs, so failed runs do not drag the average down to zero. Failed runs are still
 * reported separately via {@link #failedCount()}.
 *
 * @param totalCount       every run in the window, regardless of status
 * @param scoredCount      successful runs that produced a score
 * @param failedCount      runs that finished without a score
 * @param averageScore     mean normalized score of scored runs, absent when none exist
 * @param minScore         lowest normalized score of scored runs, absent when none exist
 * @param maxScore         highest normalized score of scored runs, absent when none exist
 */
public record EvaluationScoreSummary(
        long totalCount,
        long scoredCount,
        long failedCount,
        BigDecimal averageScore,
        BigDecimal minScore,
        BigDecimal maxScore
) {

    public static EvaluationScoreSummary empty() {
        return new EvaluationScoreSummary(0L, 0L, 0L, null, null, null);
    }

    public Optional<BigDecimal> findAverageScore() {
        return Optional.ofNullable(averageScore);
    }
}
