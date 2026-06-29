package com.api2api.domain.gateway.service;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.gateway.model.GatewayInvocationResult;
import com.api2api.domain.gateway.model.InvocationError;
import com.api2api.domain.protocol.model.ConversionResult;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.model.RoutePlan;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultGatewayInvocationService implements GatewayInvocationService {

    @Override
    public GatewayInvocation authenticate(
            GatewayInvocation invocation,
            ApiCredential credential,
            long currentConsumedTokens,
            Instant now
    ) {
        Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        Objects.requireNonNull(credential, "API credential must not be null");
        credential.assertUsable();
        credential.assertQuotaAvailable(currentConsumedTokens);
        invocation.markAuthenticated(now);
        return invocation;
    }

    @Override
    public GatewayInvocation route(GatewayInvocation invocation, RoutePlan routePlan, Instant now) {
        Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        invocation.attachRoutePlan(routePlan, now);
        return invocation;
    }

    @Override
    public GatewayInvocation recordConversion(
            GatewayInvocation invocation,
            ConversionResult conversionResult,
            boolean requestSide,
            Instant now
    ) {
        Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        if (requestSide) {
            invocation.recordRequestConversion(conversionResult, now);
        } else {
            invocation.recordResponseConversion(conversionResult, now);
        }
        return invocation;
    }

    @Override
    public GatewayInvocation completeSuccess(
            GatewayInvocation invocation,
            RouteCandidate finalCandidate,
            UnifiedTokenUsage usage,
            boolean streaming,
            Instant endedAt
    ) {
        Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        invocation.succeed(GatewayInvocationResult.success(finalCandidate, usage, streaming), endedAt);
        return invocation;
    }

    @Override
    public GatewayInvocation completeFailure(
            GatewayInvocation invocation,
            InvocationError error,
            boolean streaming,
            Instant endedAt
    ) {
        Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        invocation.fail(GatewayInvocationResult.failed(error, streaming), endedAt);
        return invocation;
    }
}
