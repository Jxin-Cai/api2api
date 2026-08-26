package com.api2api.domain.evaluation.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EvaluationScoreTest {

    @Test
    void test_rebases_raw_score_onto_100_when_denominator_is_not_100() {
        EvaluationScore score = EvaluationScore.of(new BigDecimal("18"), new BigDecimal("20"));

        assertThat(score.value()).isEqualByComparingTo("90.00");
    }

    @Test
    void test_rejects_raw_score_when_it_exceeds_max() {
        assertThatThrownBy(() -> EvaluationScore.of(new BigDecimal("21"), new BigDecimal("20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed");
    }
}
