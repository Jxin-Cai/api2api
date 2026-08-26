package com.api2api.domain.evaluation.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChannelEvaluationTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");

    @Test
    void test_marks_running_when_probe_accepts_pending_run() {
        ChannelEvaluation evaluation = pending();

        evaluation.markRunning("run-1", NOW.plusSeconds(1));

        assertThat(evaluation.status()).isEqualTo(EvaluationStatus.RUNNING);
        assertThat(evaluation.providerRunId()).contains("run-1");
        assertThat(evaluation.startedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void test_rejects_terminal_transition_when_already_succeeded() {
        ChannelEvaluation evaluation = pending();
        evaluation.markRunning("run-1", NOW.plusSeconds(1));
        evaluation.markSucceeded(outcome(), NOW.plusSeconds(2));

        assertThatThrownBy(() -> evaluation.markFailed("too late", NOW.plusSeconds(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already finished");
    }

    private static ChannelEvaluation pending() {
        return ChannelEvaluation.submit(
                ChannelEvaluationId.of(1L),
                ProviderChannelId.of(10L),
                ModelName.of("gpt-4o"),
                ProbeUpstreamFormat.OPENAI,
                EvaluationTrigger.MANUAL,
                NOW
        );
    }

    private static EvaluationOutcome outcome() {
        return EvaluationOutcome.builder()
                .score(EvaluationScore.ofNormalized(BigDecimal.valueOf(88.50)))
                .completedAt(NOW.plusSeconds(2))
                .build();
    }
}
