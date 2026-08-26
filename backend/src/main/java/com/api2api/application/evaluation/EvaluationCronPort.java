package com.api2api.application.evaluation;

import com.api2api.domain.evaluation.model.EvaluationCron;
import java.time.Instant;
import java.util.Optional;

/**
 * Outbound port that resolves cron expressions into concrete fire times, keeping the scheduling
 * library out of the domain.
 */
public interface EvaluationCronPort {

    /**
     * Computes the first fire time strictly after the given instant.
     *
     * @param cron  recurring expression and its zone
     * @param after exclusive lower bound
     * @return next fire time, or empty when the expression can never fire again
     */
    Optional<Instant> nextTriggerAfter(EvaluationCron cron, Instant after);

    /**
     * Validates that the expression is parseable, reporting a business failure when it is not.
     *
     * @param cron recurring expression and its zone
     */
    void validate(EvaluationCron cron);
}
