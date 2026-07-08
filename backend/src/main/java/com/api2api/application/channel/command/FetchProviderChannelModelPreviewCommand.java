package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import java.util.Set;
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

    private final ProviderHost host;

    private final ProviderKeyRef keyRef;

    private final ProviderModelsPath modelsPath;

    private final Set<ProtocolType> upstreamProtocols;

    @NotNull
    private final RoutePriority defaultPriority;
}
