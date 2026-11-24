package com.api2api.application.channel;

import com.api2api.application.channel.dto.ChannelTestResult;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;

public interface ProviderChannelTestPort {

    ChannelTestResult sendHello(ProviderChannel channel, ProtocolType requestProtocol, ChannelModelSupport model);
}
