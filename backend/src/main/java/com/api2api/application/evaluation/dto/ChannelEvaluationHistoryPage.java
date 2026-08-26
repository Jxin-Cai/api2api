package com.api2api.application.evaluation.dto;

import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.repository.EvaluationScoreSummary;
import java.util.List;
import java.util.Objects;

/**
 * Paged evaluation history together with windowed score statistics.
 *
 * @param evaluations matching runs in the requested order
 * @param summary     score aggregates over the same filters, ignoring paging
 * @param totalElements total matching runs
 * @param limit       page size actually applied
 * @param offset      rows skipped
 */
public record ChannelEvaluationHistoryPage(
        List<ChannelEvaluation> evaluations,
        EvaluationScoreSummary summary,
        long totalElements,
        int limit,
        int offset
) {

    public ChannelEvaluationHistoryPage {
        Objects.requireNonNull(evaluations, "Evaluations must not be null");
        Objects.requireNonNull(summary, "Evaluation score summary must not be null");
        evaluations = List.copyOf(evaluations);
    }
}
