package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Mapping from the client-facing request protocol to the upstream provider protocol used for calls.
 */
public record ChannelProtocolMapping(ProtocolType requestProtocol, ProtocolType upstreamProtocol) {

    public ChannelProtocolMapping {
        Objects.requireNonNull(requestProtocol, "Request protocol must not be null");
        Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null");
    }

    public static ChannelProtocolMapping of(ProtocolType requestProtocol, ProtocolType upstreamProtocol) {
        return new ChannelProtocolMapping(requestProtocol, upstreamProtocol);
    }
}
