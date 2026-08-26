package com.api2api.domain.evaluation.model;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Recurring evaluation trigger expression.
 *
 * <p>Holds a six-field Spring cron expression (or an {@code @macro}) together with the zone it is
 * interpreted in. Structural validation happens here; computing the next fire time is delegated to
 * the infrastructure layer so the domain stays framework free.
 */
public final class EvaluationCron {

    private static final int CRON_FIELD_COUNT = 6;

    private final String expression;
    private final ZoneId zoneId;

    private EvaluationCron(String expression, ZoneId zoneId) {
        this.expression = normalize(expression);
        this.zoneId = Objects.requireNonNull(zoneId, "Evaluation cron zone must not be null");
    }

    public static EvaluationCron of(String expression, ZoneId zoneId) {
        return new EvaluationCron(expression, zoneId);
    }

    private static String normalize(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Evaluation cron expression must not be blank");
        }
        String trimmed = expression.trim().replaceAll("\\s+", " ");
        if (trimmed.startsWith("@")) {
            return trimmed;
        }
        if (trimmed.split(" ").length != CRON_FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "Evaluation cron expression must have " + CRON_FIELD_COUNT + " fields: " + trimmed);
        }
        return trimmed;
    }

    public String expression() {
        return expression;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationCron that)) {
            return false;
        }
        return expression.equals(that.expression) && zoneId.equals(that.zoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, zoneId);
    }

    @Override
    public String toString() {
        return expression + " [" + zoneId + "]";
    }
}
