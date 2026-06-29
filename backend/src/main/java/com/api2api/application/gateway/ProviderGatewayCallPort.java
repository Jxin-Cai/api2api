package com.api2api.application.gateway;

import com.api2api.domain.protocol.model.ConversionPayload;
import com.api2api.domain.routing.model.RouteCandidate;

/**
 * Application port for forwarding a converted gateway request to the selected provider route.
 */
public interface ProviderGatewayCallPort {

    ConversionPayload forward(RouteCandidate candidate, String upstreamRequestBody, boolean streaming);
}
