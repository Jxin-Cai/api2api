package com.api2api.infr.client.provider;

import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.routing.model.RouteCandidate;
import java.util.List;
import java.util.Map;

interface ProviderCallStrategy {

    boolean supports(ProtocolType upstreamProtocol);

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
