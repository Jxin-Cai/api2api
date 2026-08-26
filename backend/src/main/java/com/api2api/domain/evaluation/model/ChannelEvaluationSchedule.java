package com.api2api.domain.evaluation.model;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

/**
 * Recurring evaluation schedule owned by a single provider channel.
 *
 * <p>An empty {@link #models()} list means "every currently enabled model of the channel", so newly
 * added models are picked up without editing the schedule.
 */
public class ChannelEvaluationSchedule {

    private final ChannelEvaluationScheduleId id;
    private final ProviderChannelId providerChannelId;
    private final Instant createdAt;

    private EvaluationCron cron;
    private List<ModelName> models;
    private boolean enabled;
    private Instant lastTriggeredAt;
    private Instant nextTriggerAt;
    private Instant updatedAt;

    @Builder(builderClassName = "Rehydrator", builderMethodName = "rehydrate")
    private ChannelEvaluationSchedule(
            ChannelEvaluationScheduleId id,
            ProviderChannelId providerChannelId,
            EvaluationCron cron,
            List<ModelName> models,
            boolean enabled,
            Instant lastTriggeredAt,
            Instant nextTriggerAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Channel evaluation schedule id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.cron = Objects.requireNonNull(cron, "Evaluation cron must not be null");
        this.models = normalizeModels(models);
        this.enabled = enabled;
        this.lastTriggeredAt = lastTriggeredAt;
        this.nextTriggerAt = nextTriggerAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
    }

    public static ChannelEvaluationSchedule create(
            ChannelEvaluationScheduleId id,
            ProviderChannelId providerChannelId,
            EvaluationCron cron,
            List<ModelName> models,
            boolean enabled,
            Instant now
    ) {
        return ChannelEvaluationSchedule.rehydrate()
                .id(id)
                .providerChannelId(providerChannelId)
                .cron(cron)
                .models(models)
                .enabled(enabled)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void reconfigure(EvaluationCron cron, List<ModelName> models, boolean enabled, Instant now) {
        Objects.requireNonNull(cron, "Evaluation cron must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        this.cron = cron;
        this.models = normalizeModels(models);
        this.enabled = enabled;
        this.updatedAt = now;
    }

    /** Stores the next fire time computed by the scheduling infrastructure. */
    public void planNextTrigger(Instant nextTriggerAt, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        this.nextTriggerAt = nextTriggerAt;
        this.updatedAt = now;
    }

    public void markTriggered(Instant triggeredAt, Instant nextTriggerAt) {
        Objects.requireNonNull(triggeredAt, "Triggered time must not be null");
        this.lastTriggeredAt = triggeredAt;
        this.nextTriggerAt = nextTriggerAt;
        this.updatedAt = triggeredAt;
    }

    public boolean isDueAt(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return enabled && nextTriggerAt != null && !nextTriggerAt.isAfter(now);
    }

    private static List<ModelName> normalizeModels(List<ModelName> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new ArrayList<>(new LinkedHashSet<>(models)));
    }

    public ChannelEvaluationScheduleId id() {
        return id;
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public EvaluationCron cron() {
        return cron;
    }

    /** Explicitly selected models, or an empty list meaning every enabled model. */
    public List<ModelName> models() {
        return models;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<Instant> lastTriggeredAt() {
        return Optional.ofNullable(lastTriggeredAt);
    }

    public Optional<Instant> nextTriggerAt() {
        return Optional.ofNullable(nextTriggerAt);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
