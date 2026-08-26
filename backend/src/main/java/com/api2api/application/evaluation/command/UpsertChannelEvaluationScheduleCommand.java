package com.api2api.application.evaluation.command;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.EvaluationCron;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for creating or replacing the recurring evaluation schedule of a channel.
 *
 * <p>An empty {@code models} list means every currently enabled model at fire time.
 */
@Getter
public final class UpsertChannelEvaluationScheduleCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final EvaluationCron cron;

    private final List<ModelName> models;
    private final boolean enabled;

    @Builder
    private UpsertChannelEvaluationScheduleCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId,
            EvaluationCron cron,
            List<ModelName> models,
            boolean enabled
    ) {
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.cron = Objects.requireNonNull(cron, "Evaluation cron must not be null");
        this.models = models == null ? List.of() : List.copyOf(models);
        this.enabled = enabled;
    }
}
