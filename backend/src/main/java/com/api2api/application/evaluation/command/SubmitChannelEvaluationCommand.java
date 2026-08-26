package com.api2api.application.evaluation.command;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for submitting one or more evaluation runs against a provider channel.
 *
 * <p>An empty {@code models} list means every currently enabled model of the channel.
 */
@Getter
public final class SubmitChannelEvaluationCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    private final List<ModelName> models;

    @Builder
    private SubmitChannelEvaluationCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId,
            List<ModelName> models
    ) {
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.models = models == null ? List.of() : List.copyOf(models);
    }
}
