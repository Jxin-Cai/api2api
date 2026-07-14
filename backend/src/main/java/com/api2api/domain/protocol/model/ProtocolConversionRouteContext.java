package com.api2api.domain.protocol.model;

/** Identifies the concrete upstream route that owns provider-bound protocol state. */
public record ProtocolConversionRouteContext(long providerChannelId, String upstreamModel) {

    public ProtocolConversionRouteContext {
        if (providerChannelId <= 0) {
            throw new IllegalArgumentException("Provider channel id must be greater than 0");
        }
        if (upstreamModel == null || upstreamModel.isBlank()) {
            throw new IllegalArgumentException("Upstream model must not be blank");
        }
    }
}
