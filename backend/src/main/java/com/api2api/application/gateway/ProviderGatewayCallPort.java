package com.api2api.application.gateway;

import com.api2api.domain.routing.model.RouteCandidate;

/**
 * Application port for forwarding a converted gateway request to the selected provider route.
 */
public interface ProviderGatewayCallPort {

    ProviderGatewayResponse forward(
            RouteCandidate candidate,
            String upstreamRequestBody,
            boolean streaming,
            InboundRequestContext inbound
    );

    ProviderStreamingResponse openStream(
            RouteCandidate candidate,
            String upstreamRequestBody,
            InboundRequestContext inbound
    );
}
