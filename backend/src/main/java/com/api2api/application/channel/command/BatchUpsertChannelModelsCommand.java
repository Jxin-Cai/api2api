package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for batch upserting provider channel model supports.
 */
@Getter
@Builder
public final class BatchUpsertChannelModelsCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final List<ChannelModelUpsertItemCommand> models;

    private final boolean replaceExisting;
}
