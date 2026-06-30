package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for manually creating or updating a provider channel model support.
 */
@Getter
@Builder
public final class UpsertChannelModelCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final ChannelModelSupportId channelModelSupportId;

    @NotNull
    private final ModelName requestedModel;

    @NotNull
    private final ModelName upstreamModel;

    @NotNull
    private final ProtocolType upstreamProtocol;

    @NotNull
    private final RoutePriority priority;

    private final boolean preferred;

    @NotNull
    private final ModelSupportSource source;
}
