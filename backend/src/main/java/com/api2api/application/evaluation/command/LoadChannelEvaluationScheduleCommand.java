package com.api2api.application.evaluation.command;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for loading the optional recurring evaluation schedule of a channel.
 */
@Getter
public final class LoadChannelEvaluationScheduleCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @Builder
    private LoadChannelEvaluationScheduleCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
    }
}
