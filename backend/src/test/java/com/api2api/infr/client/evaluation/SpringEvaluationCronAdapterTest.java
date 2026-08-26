package com.api2api.infr.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.BusinessException;
import com.api2api.domain.evaluation.model.EvaluationCron;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SpringEvaluationCronAdapterTest {

    private final SpringEvaluationCronAdapter adapter = new SpringEvaluationCronAdapter();

    @Test
    void test_computes_next_hour_when_expression_is_hourly() {
        EvaluationCron cron = EvaluationCron.of("0 0 * * * *", ZoneOffset.UTC);

        Optional<Instant> next = adapter.nextTriggerAfter(cron, Instant.parse("2026-08-21T10:15:00Z"));

        assertThat(next).contains(Instant.parse("2026-08-21T11:00:00Z"));
    }

    @Test
    void test_rejects_expression_when_cron_is_not_parseable() {
        EvaluationCron cron = EvaluationCron.of("not-a-cron 1 2 3 4 5", ZoneOffset.UTC);

        assertThatThrownBy(() -> adapter.validate(cron))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).code())
                .isEqualTo("EVALUATION_CRON_INVALID");
    }
}
