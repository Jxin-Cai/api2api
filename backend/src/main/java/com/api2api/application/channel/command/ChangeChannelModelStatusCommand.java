package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class ChangeChannelModelStatusCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final ChannelModelSupportId channelModelSupportId;
}
