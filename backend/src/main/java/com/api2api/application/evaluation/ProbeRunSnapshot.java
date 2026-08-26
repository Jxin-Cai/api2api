package com.api2api.application.evaluation;

import com.api2api.domain.evaluation.model.EvaluationOutcome;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import java.util.Objects;
import java.util.Optional;

/**
 * Current state of a probe run as reported by the probe service.
 *
 * <p>Exactly one of {@code outcome} or {@code failureReason} is populated for a terminal status;
 * both are absent while the run is still executing.
 *
 * @param status        mapped lifecycle status
 * @param outcome       result details, present only when the run succeeded
 * @param failureReason human readable reason, present only when the run failed
 */
public record ProbeRunSnapshot(
        EvaluationStatus status,
        EvaluationOutcome outcome,
        String failureReason
) {

    public ProbeRunSnapshot {
        Objects.requireNonNull(status, "Evaluation status must not be null");
    }

    public static ProbeRunSnapshot running() {
        return new ProbeRunSnapshot(EvaluationStatus.RUNNING, null, null);
    }

    public static ProbeRunSnapshot succeeded(EvaluationOutcome outcome) {
        return new ProbeRunSnapshot(EvaluationStatus.SUCCEEDED, Objects.requireNonNull(outcome), null);
    }

    public static ProbeRunSnapshot failed(String failureReason) {
        return new ProbeRunSnapshot(EvaluationStatus.FAILED, null, failureReason);
    }

    public Optional<EvaluationOutcome> findOutcome() {
        return Optional.ofNullable(outcome);
    }
}
