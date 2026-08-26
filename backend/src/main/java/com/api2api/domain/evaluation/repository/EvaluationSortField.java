package com.api2api.domain.evaluation.repository;

import java.util.Locale;
import java.util.Optional;

/**
 * Sortable columns of the evaluation history listing.
 */
public enum EvaluationSortField {

    /** Submission time, the default chronological ordering. */
    REQUESTED_AT,

    /** Normalized 0-100 score, so runs with different denominators rank consistently. */
    SCORE;

    public static Optional<EvaluationSortField> parse(String value) {
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
