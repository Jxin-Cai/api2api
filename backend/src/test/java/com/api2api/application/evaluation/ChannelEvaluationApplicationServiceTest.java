package com.api2api.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.application.BusinessException;
import com.api2api.application.evaluation.command.SubmitChannelEvaluationCommand;
import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ChannelProtocolMapping;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderChannelStatus;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.model.EvaluationOutcome;
import com.api2api.domain.evaluation.model.EvaluationScore;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import com.api2api.domain.evaluation.model.EvaluationTrigger;
import com.api2api.domain.evaluation.repository.ChannelEvaluationRepository;
import com.api2api.domain.evaluation.repository.ChannelEvaluationScheduleRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChannelEvaluationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UserAccountId OPERATOR_ID = UserAccountId.of(1L);
    private static final ProviderChannelId CHANNEL_ID = ProviderChannelId.of(10L);

    @Test
    void test_submits_enabled_models_when_model_list_is_empty() {
        UserAccount operator = mock(UserAccount.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        ProviderChannelRepository channels = mock(ProviderChannelRepository.class);
        ChannelEvaluationRepository evaluations = mock(ChannelEvaluationRepository.class);
        ChannelEvaluationProbePort probePort = mock(ChannelEvaluationProbePort.class);
        when(users.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
        when(channels.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithEnabledModel()));
        when(probePort.submit(any())).thenReturn("probe-run-1");
        ChannelEvaluationApplicationService service = service(users, channels, evaluations, probePort);

        List<ChannelEvaluation> submitted = service.submit(SubmitChannelEvaluationCommand.builder()
                .operatorUserId(OPERATOR_ID)
                .providerChannelId(CHANNEL_ID)
                .models(List.of())
                .build());

        verify(operator).assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
        assertThat(submitted).hasSize(1);
        assertThat(submitted.get(0).status()).isEqualTo(EvaluationStatus.RUNNING);
        assertThat(submitted.get(0).providerRunId()).contains("probe-run-1");
        ArgumentCaptor<ProbeSubmission> submission = ArgumentCaptor.forClass(ProbeSubmission.class);
        verify(probePort).submit(submission.capture());
        assertThat(submission.getValue().modelId().value()).isEqualTo("gpt-4o");
        verify(evaluations).save(submitted.get(0));
    }

    @Test
    void test_rejects_submit_when_requested_model_is_not_enabled() {
        UserAccount operator = mock(UserAccount.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        ProviderChannelRepository channels = mock(ProviderChannelRepository.class);
        ChannelEvaluationRepository evaluations = mock(ChannelEvaluationRepository.class);
        ChannelEvaluationProbePort probePort = mock(ChannelEvaluationProbePort.class);
        when(users.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
        when(channels.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithEnabledModel()));
        ChannelEvaluationApplicationService service = service(users, channels, evaluations, probePort);

        assertThatThrownBy(() -> service.submit(SubmitChannelEvaluationCommand.builder()
                .operatorUserId(OPERATOR_ID)
                .providerChannelId(CHANNEL_ID)
                .models(List.of(ModelName.of("missing-model")))
                .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).code())
                .isEqualTo("EVALUATION_MODEL_NOT_ENABLED");
        verify(probePort, never()).submit(any());
        verify(evaluations, never()).save(any());
    }

    @Test
    void test_marks_in_flight_run_succeeded_when_probe_reports_score() {
        ChannelEvaluationRepository evaluations = mock(ChannelEvaluationRepository.class);
        ChannelEvaluationProbePort probePort = mock(ChannelEvaluationProbePort.class);
        ChannelEvaluation running = runningEvaluation();
        when(evaluations.findInFlight(20)).thenReturn(List.of(running));
        when(probePort.fetch("probe-run-1")).thenReturn(ProbeRunSnapshot.succeeded(EvaluationOutcome.builder()
                .score(EvaluationScore.ofNormalized(BigDecimal.valueOf(91)))
                .completedAt(NOW.plusSeconds(30))
                .build()));
        ChannelEvaluationApplicationService service = service(
                mock(UserAccountRepository.class),
                mock(ProviderChannelRepository.class),
                evaluations,
                probePort
        );

        int refreshed = service.refreshInFlight(20, Duration.ofMinutes(30));

        assertThat(refreshed).isEqualTo(1);
        assertThat(running.status()).isEqualTo(EvaluationStatus.SUCCEEDED);
        assertThat(running.outcome()).isPresent();
        verify(evaluations).save(running);
    }

    private ChannelEvaluationApplicationService service(
            UserAccountRepository users,
            ProviderChannelRepository channels,
            ChannelEvaluationRepository evaluations,
            ChannelEvaluationProbePort probePort
    ) {
        return new ChannelEvaluationApplicationService(
                users,
                channels,
                evaluations,
                mock(ChannelEvaluationScheduleRepository.class),
                probePort,
                mock(EvaluationCronPort.class),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static ChannelEvaluation runningEvaluation() {
        ChannelEvaluation evaluation = ChannelEvaluation.submit(
                com.api2api.domain.evaluation.model.ChannelEvaluationId.of(99L),
                CHANNEL_ID,
                ModelName.of("gpt-4o"),
                com.api2api.domain.evaluation.model.ProbeUpstreamFormat.OPENAI,
                EvaluationTrigger.MANUAL,
                NOW
        );
        evaluation.markRunning("probe-run-1", NOW);
        return evaluation;
    }

    private static ProviderChannel channelWithEnabledModel() {
        return ProviderChannel.rehydrate(
                CHANNEL_ID,
                ProviderChannelName.of("probe-channel"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("key-ref"),
                ProviderModelsPath.DEFAULT,
                0,
                Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                List.of(ChannelModelSupport.rehydrate(
                        ChannelModelSupportId.of(1L),
                        ModelName.of("gpt-4o"),
                        ModelName.of("gpt-4o"),
                        ProtocolType.OPENAI_RESPONSES,
                        RoutePriority.of(1),
                        false,
                        ChannelModelStatus.ENABLED,
                        null,
                        null,
                        ModelSupportSource.MANUAL,
                        NOW,
                        NOW
                )),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );
    }
}
