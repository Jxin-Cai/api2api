package com.api2api.application.channel;

import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.RoutePriority;
import java.util.List;
import java.util.Set;

/**
 * Application port for fetching model capabilities from an upstream provider channel.
 */
public interface ProviderModelFetchPort {

    List<ChannelModelSupport> fetchModels(
            ProviderChannelId channelId,
            ProviderHost host,
            ProviderKeyRef keyRef,
            Set<ProtocolType> supportedProtocols,
            RoutePriority defaultPriority
    );
}
