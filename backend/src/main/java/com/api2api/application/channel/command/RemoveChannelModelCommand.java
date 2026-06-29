package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for removing a provider channel model support.
 */
@Getter
@Builder
public final class RemoveChannelModelCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final ModelName requestedModel;

    @NotNull
    private final ProtocolType upstreamProtocol;
}
