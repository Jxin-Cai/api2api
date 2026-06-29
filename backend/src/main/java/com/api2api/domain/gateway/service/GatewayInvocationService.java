package com.api2api.domain.gateway.service;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.gateway.model.InvocationError;
import com.api2api.domain.protocol.model.ConversionResult;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.model.RoutePlan;
import java.time.Instant;

/**
 * Domain service contract for advancing a gateway invocation through authentication,
 * routing, conversion and terminal result rules.
 */
public interface GatewayInvocationService {

    GatewayInvocation authenticate(
            GatewayInvocation invocation,
            ApiCredential credential,
            long currentConsumedTokens,
            Instant now
    );

    GatewayInvocation route(GatewayInvocation invocation, RoutePlan routePlan, Instant now);

    GatewayInvocation recordConversion(
            GatewayInvocation invocation,
            ConversionResult conversionResult,
            boolean requestSide,
            Instant now
    );

    GatewayInvocation completeSuccess(
            GatewayInvocation invocation,
            RouteCandidate finalCandidate,
            UnifiedTokenUsage usage,
            boolean streaming,
            Instant endedAt
    );

    GatewayInvocation completeFailure(
            GatewayInvocation invocation,
            InvocationError error,
            boolean streaming,
            Instant endedAt
    );
}
