package com.api2api.domain.routing.service;

import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.routing.model.FailoverDecision;
import com.api2api.domain.routing.model.RouteAttempt;
import com.api2api.domain.routing.model.RouteFailure;
import com.api2api.domain.routing.model.RoutePlan;
import com.api2api.domain.routing.model.RoutingRequest;
import java.time.Instant;
import java.util.List;

/**
 * Routing policy domain service contract.
 */
public interface RoutingPolicyService {

    RoutePlan buildRoutePlan(
            RoutingRequest request,
            List<ProviderChannel> channels,
            List<ProtocolConversionDefinition> conversionDefinitions,
            Instant now
    );

    FailoverDecision decideNext(
            RoutePlan routePlan,
            List<RouteAttempt> attempts,
            RouteFailure latestFailure
    );
}
