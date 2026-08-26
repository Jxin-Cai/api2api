package com.api2api.application.evaluation;

import com.api2api.application.BusinessException;
import com.api2api.application.evaluation.command.LoadChannelEvaluationScheduleCommand;
import com.api2api.application.evaluation.command.QueryChannelEvaluationHistoryCommand;
import com.api2api.application.evaluation.command.SubmitChannelEvaluationCommand;
import com.api2api.application.evaluation.command.UpsertChannelEvaluationScheduleCommand;
import com.api2api.application.evaluation.dto.ChannelEvaluationHistoryPage;
import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.model.ChannelEvaluationId;
import com.api2api.domain.evaluation.model.ChannelEvaluationSchedule;
import com.api2api.domain.evaluation.model.ChannelEvaluationScheduleId;
import com.api2api.domain.evaluation.model.EvaluationOutcome;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import com.api2api.domain.evaluation.model.EvaluationTrigger;
import com.api2api.domain.evaluation.model.ProbeUpstreamFormat;
import com.api2api.domain.evaluation.repository.ChannelEvaluationRepository;
import com.api2api.domain.evaluation.repository.ChannelEvaluationScheduleRepository;
import com.api2api.domain.evaluation.repository.EvaluationHistoryQuery;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates channel evaluation runs and their optional recurring schedule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelEvaluationApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final ProviderChannelRepository providerChannelRepository;

    @NonNull
    private final ChannelEvaluationRepository channelEvaluationRepository;

    @NonNull
    private final ChannelEvaluationScheduleRepository channelEvaluationScheduleRepository;

    @NonNull
    private final ChannelEvaluationProbePort probePort;

    @NonNull
    private final EvaluationCronPort cronPort;

    @NonNull
    private final Clock clock;

    @Transactional(rollbackFor = Exception.class)
    public List<ChannelEvaluation> submit(SubmitChannelEvaluationCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());
        return submitModels(channel, resolveModels(channel, command.getModels()), EvaluationTrigger.MANUAL);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ChannelEvaluationHistoryPage history(QueryChannelEvaluationHistoryCommand command) {
        assertAdmin(command.getOperatorUserId());
        loadChannel(command.getProviderChannelId());
        EvaluationHistoryQuery query = EvaluationHistoryQuery.builder()
                .providerChannelId(command.getProviderChannelId())
                .requestedModel(command.getRequestedModel())
                .status(command.getStatus())
                .from(command.getFrom())
                .to(command.getTo())
                .sortField(command.getSortField())
                .descending(command.isDescending())
                .limit(command.getLimit())
                .offset(command.getOffset())
                .build();
        return new ChannelEvaluationHistoryPage(
                channelEvaluationRepository.findHistory(query),
                channelEvaluationRepository.summarize(query),
                channelEvaluationRepository.countHistory(query),
                query.limit(),
                query.offset()
        );
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<ChannelEvaluationSchedule> loadSchedule(LoadChannelEvaluationScheduleCommand command) {
        assertAdmin(command.getOperatorUserId());
        loadChannel(command.getProviderChannelId());
        return channelEvaluationScheduleRepository.findByProviderChannelId(command.getProviderChannelId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ChannelEvaluationSchedule upsertSchedule(UpsertChannelEvaluationScheduleCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());
        cronPort.validate(command.getCron());
        Instant now = now();
        ChannelEvaluationSchedule schedule = channelEvaluationScheduleRepository
                .findByProviderChannelId(channel.id())
                .orElseGet(() -> ChannelEvaluationSchedule.create(
                        nextScheduleId(),
                        channel.id(),
                        command.getCron(),
                        command.getModels(),
                        command.isEnabled(),
                        now
                ));
        if (!schedule.providerChannelId().equals(channel.id())
                || !schedule.cron().equals(command.getCron())
                || !schedule.models().equals(command.getModels())
                || schedule.isEnabled() != command.isEnabled()) {
            schedule.reconfigure(command.getCron(), command.getModels(), command.isEnabled(), now);
        }
        Instant nextTrigger = command.isEnabled()
                ? cronPort.nextTriggerAfter(command.getCron(), now).orElse(null)
                : null;
        schedule.planNextTrigger(nextTrigger, now);
        channelEvaluationScheduleRepository.save(schedule);
        return schedule;
    }

    @Transactional(rollbackFor = Exception.class)
    public int refreshInFlight(int limit, Duration runTimeout) {
        Objects.requireNonNull(runTimeout, "Evaluation run timeout must not be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("Evaluation poll batch size must be positive");
        }
        Instant now = now();
        int refreshed = 0;
        for (ChannelEvaluation evaluation : channelEvaluationRepository.findInFlight(limit)) {
            try {
                refresh(evaluation, now, runTimeout);
                refreshed++;
            } catch (RuntimeException exception) {
                log.warn("Failed to refresh channel evaluation {}", evaluation.id().value(), exception);
            }
        }
        return refreshed;
    }

    @Transactional(rollbackFor = Exception.class)
    public int fireDueSchedules(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Evaluation schedule batch size must be positive");
        }
        Instant now = now();
        int fired = 0;
        for (ChannelEvaluationSchedule schedule : channelEvaluationScheduleRepository.findDue(now, limit)) {
            try {
                fire(schedule, now);
                fired++;
            } catch (RuntimeException exception) {
                log.warn("Failed to fire channel evaluation schedule {}", schedule.id().value(), exception);
            }
        }
        return fired;
    }

    private List<ChannelEvaluation> submitModels(
            ProviderChannel channel,
            List<ChannelModelSupport> models,
            EvaluationTrigger trigger
    ) {
        if (models.isEmpty()) {
            throw new BusinessException("EVALUATION_MODELS_EMPTY", "渠道没有可测评的启用模型");
        }
        Instant now = now();
        List<ChannelEvaluation> submitted = new ArrayList<>();
        for (ChannelModelSupport model : models) {
            ChannelEvaluation evaluation = ChannelEvaluation.submit(
                    nextEvaluationId(),
                    channel.id(),
                    model.requestedModel(),
                    ProbeUpstreamFormat.fromProtocol(model.upstreamProtocol()),
                    trigger,
                    now
            );
            try {
                String providerRunId = probePort.submit(ProbeSubmission.builder()
                        .host(channel.host())
                        .keyRef(channel.keyRef())
                        .modelId(model.upstreamModel())
                        .upstreamFormat(evaluation.upstreamFormat())
                        .build());
                evaluation.markRunning(providerRunId, now);
            } catch (BusinessException exception) {
                evaluation.markFailed(exception.getMessage(), now);
            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to submit channel evaluation for channel {} model {}",
                        channel.id().value(),
                        model.requestedModel().value(),
                        exception
                );
                evaluation.markFailed(exception.getMessage(), now);
            }
            channelEvaluationRepository.save(evaluation);
            submitted.add(evaluation);
        }
        return submitted;
    }

    private void refresh(ChannelEvaluation evaluation, Instant now, Duration runTimeout) {
        if (evaluation.status() == EvaluationStatus.PENDING) {
            evaluation.markFailed("测评任务尚未被探测服务接受", now);
            channelEvaluationRepository.save(evaluation);
            return;
        }
        String providerRunId = evaluation.providerRunId().orElse(null);
        if (providerRunId == null || providerRunId.isBlank()) {
            evaluation.markFailed("测评任务缺少探测服务运行标识", now);
            channelEvaluationRepository.save(evaluation);
            return;
        }
        Instant deadline = evaluation.requestedAt().plus(runTimeout);
        try {
            ProbeRunSnapshot snapshot = probePort.fetch(providerRunId);
            applySnapshot(evaluation, snapshot, now, deadline);
        } catch (BusinessException exception) {
            if (!now.isBefore(deadline)) {
                evaluation.markFailed(exception.getMessage(), now);
                channelEvaluationRepository.save(evaluation);
            } else {
                log.warn("Transient probe fetch failure for evaluation {}", evaluation.id().value(), exception);
            }
        }
    }

    private void applySnapshot(
            ChannelEvaluation evaluation,
            ProbeRunSnapshot snapshot,
            Instant now,
            Instant deadline
    ) {
        if (snapshot.status() == EvaluationStatus.SUCCEEDED) {
            EvaluationOutcome outcome = snapshot.findOutcome()
                    .orElseThrow(() -> new BusinessException("EVALUATION_OUTCOME_MISSING", "探测服务成功响应缺少评分结果"));
            evaluation.markSucceeded(outcome, now);
            channelEvaluationRepository.save(evaluation);
            return;
        }
        if (snapshot.status() == EvaluationStatus.FAILED) {
            evaluation.markFailed(snapshot.failureReason(), now);
            channelEvaluationRepository.save(evaluation);
            return;
        }
        if (!now.isBefore(deadline)) {
            evaluation.markFailed("测评超时，探测服务未在约定时间内返回结果", now);
            channelEvaluationRepository.save(evaluation);
        }
    }

    private void fire(ChannelEvaluationSchedule schedule, Instant now) {
        Instant nextTrigger = cronPort.nextTriggerAfter(schedule.cron(), now).orElse(null);
        schedule.markTriggered(now, nextTrigger);
        channelEvaluationScheduleRepository.save(schedule);
        ProviderChannel channel = loadChannel(schedule.providerChannelId());
        submitModels(channel, resolveModels(channel, schedule.models()), EvaluationTrigger.SCHEDULED);
    }

    private List<ChannelModelSupport> resolveModels(ProviderChannel channel, List<ModelName> requestedModels) {
        Map<ModelName, ChannelModelSupport> enabled = new LinkedHashMap<>();
        for (ChannelModelSupport model : channel.supportedModels()) {
            if (model.status() != ChannelModelStatus.ENABLED) {
                continue;
            }
            enabled.putIfAbsent(model.requestedModel(), model);
        }
        if (requestedModels == null || requestedModels.isEmpty()) {
            return List.copyOf(enabled.values());
        }
        Set<ModelName> uniqueRequested = new LinkedHashSet<>(requestedModels);
        List<ChannelModelSupport> resolved = new ArrayList<>();
        for (ModelName requested : uniqueRequested) {
            ChannelModelSupport model = enabled.get(requested);
            if (model == null) {
                throw new BusinessException("EVALUATION_MODEL_NOT_ENABLED", "模型未启用或不属于该渠道：" + requested.value());
            }
            resolved.add(model);
        }
        return resolved;
    }

    private void assertAdmin(UserAccountId operatorUserId) {
        UserAccount operator = userAccountRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }

    private ProviderChannel loadChannel(ProviderChannelId providerChannelId) {
        return providerChannelRepository.findById(providerChannelId)
                .orElseThrow(() -> new BusinessException("PROVIDER_CHANNEL_NOT_FOUND"));
    }

    private ChannelEvaluationId nextEvaluationId() {
        return ChannelEvaluationId.of(nextId());
    }

    private ChannelEvaluationScheduleId nextScheduleId() {
        return ChannelEvaluationScheduleId.of(nextId());
    }

    private long nextId() {
        long timestampPart = clock.millis() * 1_000L;
        long randomPart = ThreadLocalRandom.current().nextLong(1_000L);
        return timestampPart + randomPart;
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
