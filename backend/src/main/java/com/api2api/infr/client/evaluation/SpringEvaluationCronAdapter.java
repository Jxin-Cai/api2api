package com.api2api.infr.client.evaluation;

import com.api2api.application.BusinessException;
import com.api2api.application.evaluation.EvaluationCronPort;
import com.api2api.domain.evaluation.model.EvaluationCron;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/**
 * Resolves six-field Spring cron expressions without leaking the scheduling library into the domain.
 */
@Component
public class SpringEvaluationCronAdapter implements EvaluationCronPort {

    @Override
    public Optional<Instant> nextTriggerAfter(EvaluationCron cron, Instant after) {
        Objects.requireNonNull(cron, "Evaluation cron must not be null");
        Objects.requireNonNull(after, "Exclusive lower bound must not be null");
        CronExpression expression = parse(cron);
        ZonedDateTime next = expression.next(after.atZone(cron.zoneId()));
        return next == null ? Optional.empty() : Optional.of(next.toInstant());
    }

    @Override
    public void validate(EvaluationCron cron) {
        parse(cron);
    }

    private CronExpression parse(EvaluationCron cron) {
        Objects.requireNonNull(cron, "Evaluation cron must not be null");
        try {
            return CronExpression.parse(expand(cron.expression()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("EVALUATION_CRON_INVALID", "Cron 表达式无法解析：" + cron.expression(), exception);
        }
    }

    private static String expand(String expression) {
        return switch (expression) {
            case "@yearly", "@annually" -> "0 0 0 1 1 *";
            case "@monthly" -> "0 0 0 1 * *";
            case "@weekly" -> "0 0 0 * * 0";
            case "@daily", "@midnight" -> "0 0 0 * * *";
            case "@hourly" -> "0 0 * * * *";
            default -> expression;
        };
    }
}
