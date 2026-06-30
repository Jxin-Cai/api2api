package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for previewing upstream models from a saved provider channel without persisting them.
 */
@Getter
@Builder
public final class FetchProviderChannelModelPreviewCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final RoutePriority defaultPriority;
}
