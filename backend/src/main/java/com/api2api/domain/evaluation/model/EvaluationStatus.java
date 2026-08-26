package com.api2api.domain.evaluation.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Lifecycle status of an asynchronous channel evaluation run.
 */
public enum EvaluationStatus {

    /** Persisted locally but not yet accepted by the probe service. */
    PENDING,

    /** Accepted by the probe service and still executing upstream. */
    RUNNING,

    /** Finished upstream with a usable score. */
    SUCCEEDED,

    /** Finished upstream or locally without a usable score. */
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }

    public boolean isInFlight() {
        return !isTerminal();
    }

    public static Optional<EvaluationStatus> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
