package com.api2api.infr.client.provider;

import com.api2api.application.gateway.InboundRequestContext;
import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.routing.model.RouteCandidate;

interface ProviderCallStrategy {

    boolean supports(ProtocolType upstreamProtocol);

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
