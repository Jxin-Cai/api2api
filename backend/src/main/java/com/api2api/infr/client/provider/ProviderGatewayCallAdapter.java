package com.api2api.infr.client.provider;

import com.api2api.application.gateway.ProviderGatewayCallPort;
import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.domain.routing.model.RouteCandidate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProviderGatewayCallAdapter implements ProviderGatewayCallPort {

    private final List<ProviderCallStrategy> strategies;

    public ProviderGatewayCallAdapter(List<ProviderCallStrategy> strategies) {
        this.strategies = List.copyOf(Objects.requireNonNull(strategies, "Provider call strategies must not be null"));
    }

    @Override
    public ProviderGatewayResponse forward(
            RouteCandidate candidate,
            String upstreamRequestBody,
            boolean streaming,
            Map<String, List<String>> incomingHeaders
    ) {
        Objects.requireNonNull(candidate, "Route candidate must not be null");
        Objects.requireNonNull(upstreamRequestBody, "Upstream request body must not be null");
        return findStrategy(candidate).forward(candidate, upstreamRequestBody, streaming, incomingHeaders);
    }

    @Override
    public ProviderStreamingResponse openStream(
            RouteCandidate candidate,
            String upstreamRequestBody,
            Map<String, List<String>> incomingHeaders
    ) {
        Objects.requireNonNull(candidate, "Route candidate must not be null");
        Objects.requireNonNull(upstreamRequestBody, "Upstream request body must not be null");
        return findStrategy(candidate).openStream(candidate, upstreamRequestBody, incomingHeaders);
    }

    private ProviderCallStrategy findStrategy(RouteCandidate candidate) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(candidate.upstreamProtocol()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No provider call strategy found for protocol: " + candidate.upstreamProtocol()));
    }
}
