package com.api2api.domain.evaluation.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Describes what caused an evaluation run to be submitted.
 */
public enum EvaluationTrigger {

    /** Submitted explicitly from the admin console. */
    MANUAL,

    /** Submitted by the recurring schedule runner. */
    SCHEDULED;

    public static Optional<EvaluationTrigger> parse(String value) {
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
