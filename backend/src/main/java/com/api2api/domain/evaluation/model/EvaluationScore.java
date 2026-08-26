package com.api2api.domain.evaluation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Probe score of a single evaluation run, always expressed on a 0-100 scale.
 *
 * <p>The probe service reports a raw score together with the denominator it used. Rebasing at
 * ingestion keeps ranking and windowed averages comparable even if that denominator changes.
 */
public final class EvaluationScore {

    private static final int SCALE = 2;
    private static final BigDecimal FULL_MARK = BigDecimal.valueOf(100);

    private final BigDecimal value;

    private EvaluationScore(BigDecimal value) {
        this.value = value;
    }

    /**
     * Rebases a raw probe score onto 0-100.
     *
     * @param rawScore score reported by the probe service
     * @param rawMax   denominator the probe service used
     */
    public static EvaluationScore of(BigDecimal rawScore, BigDecimal rawMax) {
        Objects.requireNonNull(rawScore, "Raw evaluation score must not be null");
        Objects.requireNonNull(rawMax, "Raw evaluation score max must not be null");
        if (rawMax.signum() <= 0) {
            throw new IllegalArgumentException("Raw evaluation score max must be greater than 0");
        }
        if (rawScore.signum() < 0) {
            throw new IllegalArgumentException("Raw evaluation score must not be negative");
        }
        if (rawScore.compareTo(rawMax) > 0) {
            throw new IllegalArgumentException("Raw evaluation score must not exceed its max");
        }
        return new EvaluationScore(rawScore.multiply(FULL_MARK).divide(rawMax, SCALE, RoundingMode.HALF_UP));
    }

    /** Rebuilds a score that was already rebased, for example when reading it back from storage. */
    public static EvaluationScore ofNormalized(BigDecimal normalized) {
        Objects.requireNonNull(normalized, "Normalized evaluation score must not be null");
        if (normalized.signum() < 0 || normalized.compareTo(FULL_MARK) > 0) {
            throw new IllegalArgumentException("Normalized evaluation score must be between 0 and 100");
        }
        return new EvaluationScore(normalized.setScale(SCALE, RoundingMode.HALF_UP));
    }

    /** Score on a 0-100 scale with two decimals. */
    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationScore that)) {
            return false;
        }
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
