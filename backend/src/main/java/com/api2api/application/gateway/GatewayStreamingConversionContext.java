package com.api2api.application.gateway;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;

/**
 * Immutable client-facing context required while converting an upstream stream.
 */
public record GatewayStreamingConversionContext(
        ProtocolType upstreamProtocol,
        ProtocolType clientProtocol,
        ModelName clientModel
) {

    public GatewayStreamingConversionContext {
        Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null");
        Objects.requireNonNull(clientProtocol, "Client protocol must not be null");
        Objects.requireNonNull(clientModel, "Client model must not be null");
    }

    public static GatewayStreamingConversionContext of(
            ProtocolType upstreamProtocol,
            ProtocolType clientProtocol,
            ModelName clientModel
    ) {
        return new GatewayStreamingConversionContext(upstreamProtocol, clientProtocol, clientModel);
    }
}
