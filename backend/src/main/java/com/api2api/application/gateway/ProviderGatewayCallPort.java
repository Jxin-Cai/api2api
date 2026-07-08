package com.api2api.application.gateway;

import com.api2api.domain.routing.model.RouteCandidate;
import java.util.List;
import java.util.Map;

/**
 * Application port for forwarding a converted gateway request to the selected provider route.
 */
public interface ProviderGatewayCallPort {

    ProviderGatewayResponse forward(
            RouteCandidate candidate,
            String upstreamRequestBody,
            boolean streaming,
            Map<String, List<String>> incomingHeaders
    );

    ProviderStreamingResponse openStream(
            RouteCandidate candidate,
            String upstreamRequestBody,
            Map<String, List<String>> incomingHeaders
    );
}
