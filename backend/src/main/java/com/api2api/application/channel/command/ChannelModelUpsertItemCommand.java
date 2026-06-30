package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.RoutePriority;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Model support item for batch upsert commands.
 */
@Getter
@Builder
public final class ChannelModelUpsertItemCommand {

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
