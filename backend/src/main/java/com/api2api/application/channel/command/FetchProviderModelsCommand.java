package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for fetching upstream provider models and replacing channel model supports.
 */
@Getter
@Builder
public final class FetchProviderModelsCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final RoutePriority defaultPriority;
}
