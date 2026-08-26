package com.api2api.domain.evaluation.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

/**
 * Result details captured from a finished probe run.
 *
 * @param score              probe score, always present for a successful run
 * @param detectedFamily     model family the probe believes actually served the request
 * @param detectedModel      concrete model the probe believes actually served the request
 * @param detectedConfidence probe confidence in the detected model, between 0 and 1
 * @param familyMismatch     whether the detected family differs from the requested model's family
 * @param channelSignature   upstream gateway fingerprint, for example {@code one-api}
 * @param reportUrl          human readable report location on the probe service
 * @param reportSummary      compacted probe report persisted as a JSON document
 * @param completedAt        upstream completion time
 */
@Builder
public record EvaluationOutcome(
        EvaluationScore score,
        String detectedFamily,
        String detectedModel,
        BigDecimal detectedConfidence,
        Boolean familyMismatch,
        String channelSignature,
        String reportUrl,
        Integer passedProbeCount,
        Integer warningProbeCount,
        Integer failedProbeCount,
        Long totalInputTokens,
        Long totalOutputTokens,
        String reportSummary,
        Instant completedAt
) {

    public EvaluationOutcome {
        Objects.requireNonNull(score, "Evaluation score must not be null");
    }
}
