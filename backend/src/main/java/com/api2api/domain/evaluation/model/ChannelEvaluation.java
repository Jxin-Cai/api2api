package com.api2api.domain.evaluation.model;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

/**
 * Aggregate root representing one asynchronous evaluation run of a channel model.
 *
 * <p>A run starts as {@link EvaluationStatus#PENDING}, becomes {@link EvaluationStatus#RUNNING} once
 * the probe service accepts it, and finally settles as {@link EvaluationStatus#SUCCEEDED} with an
 * {@link EvaluationOutcome} or {@link EvaluationStatus#FAILED} with a reason. Terminal runs are
 * immutable.
 */
public class ChannelEvaluation {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final ChannelEvaluationId id;
    private final ProviderChannelId providerChannelId;
    private final ModelName requestedModel;
    private final ProbeUpstreamFormat upstreamFormat;
    private final EvaluationTrigger trigger;
    private final Instant requestedAt;
    private final Instant createdAt;

    private String providerRunId;
    private EvaluationStatus status;
    private EvaluationOutcome outcome;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private Instant updatedAt;

    @Builder(builderClassName = "Rehydrator", builderMethodName = "rehydrate")
    private ChannelEvaluation(
            ChannelEvaluationId id,
            ProviderChannelId providerChannelId,
            ModelName requestedModel,
            ProbeUpstreamFormat upstreamFormat,
            EvaluationTrigger trigger,
            String providerRunId,
            EvaluationStatus status,
            EvaluationOutcome outcome,
            String errorMessage,
            Instant requestedAt,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Channel evaluation id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.requestedModel = Objects.requireNonNull(requestedModel, "Requested model must not be null");
        this.upstreamFormat = Objects.requireNonNull(upstreamFormat, "Probe upstream format must not be null");
        this.trigger = Objects.requireNonNull(trigger, "Evaluation trigger must not be null");
        this.status = Objects.requireNonNull(status, "Evaluation status must not be null");
        this.requestedAt = Objects.requireNonNull(requestedAt, "Requested time must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
        this.providerRunId = providerRunId;
        this.outcome = outcome;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static ChannelEvaluation submit(
            ChannelEvaluationId id,
            ProviderChannelId providerChannelId,
            ModelName requestedModel,
            ProbeUpstreamFormat upstreamFormat,
            EvaluationTrigger trigger,
            Instant now
    ) {
        return ChannelEvaluation.rehydrate()
                .id(id)
                .providerChannelId(providerChannelId)
                .requestedModel(requestedModel)
                .upstreamFormat(upstreamFormat)
                .trigger(trigger)
                .status(EvaluationStatus.PENDING)
                .requestedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** Records that the probe service accepted the run and returned its own run identifier. */
    public void markRunning(String providerRunId, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (providerRunId == null || providerRunId.isBlank()) {
            throw new IllegalArgumentException("Provider run id must not be blank");
        }
        ensureNotTerminal();
        this.providerRunId = providerRunId.trim();
        this.status = EvaluationStatus.RUNNING;
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void markSucceeded(EvaluationOutcome outcome, Instant now) {
        Objects.requireNonNull(outcome, "Evaluation outcome must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        ensureNotTerminal();
        this.outcome = outcome;
        this.status = EvaluationStatus.SUCCEEDED;
        this.errorMessage = null;
        this.completedAt = outcome.completedAt() == null ? now : outcome.completedAt();
        this.updatedAt = now;
    }

    public void markFailed(String reason, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        ensureNotTerminal();
        this.status = EvaluationStatus.FAILED;
        this.errorMessage = truncate(reason);
        this.completedAt = now;
        this.updatedAt = now;
    }

    private void ensureNotTerminal() {
        if (status.isTerminal()) {
            throw new IllegalStateException("Channel evaluation " + id + " already finished as " + status);
        }
    }

    private static String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    public ChannelEvaluationId id() {
        return id;
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ProbeUpstreamFormat upstreamFormat() {
        return upstreamFormat;
    }

    public EvaluationTrigger trigger() {
        return trigger;
    }

    public Optional<String> providerRunId() {
        return Optional.ofNullable(providerRunId);
    }

    public EvaluationStatus status() {
        return status;
    }

    public Optional<EvaluationOutcome> outcome() {
        return Optional.ofNullable(outcome);
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
