package com.api2api.application.gateway;

import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.usage.model.UsageRecordId;
import java.util.Objects;

/**
 * Result of preparing a streaming gateway invocation before bytes are written to the client.
 */
public final class GatewayStreamingInvocation {

    private final GatewayInvocation invocation;
    private final UsageRecordId usageRecordId;
    private final RouteCandidate candidate;
    private final ProviderStreamingResponse providerResponse;

    private GatewayStreamingInvocation(
            GatewayInvocation invocation,
            UsageRecordId usageRecordId,
            RouteCandidate candidate,
            ProviderStreamingResponse providerResponse
    ) {
        this.invocation = Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        this.usageRecordId = Objects.requireNonNull(usageRecordId, "Usage record id must not be null");
        this.candidate = candidate;
        this.providerResponse = providerResponse;
        if ((candidate == null) != (providerResponse == null)) {
            throw new IllegalArgumentException("Streaming candidate and provider response must both be present or absent");
        }
    }

    public static GatewayStreamingInvocation opened(
            GatewayInvocation invocation,
            UsageRecordId usageRecordId,
            RouteCandidate candidate,
            ProviderStreamingResponse providerResponse
    ) {
        return new GatewayStreamingInvocation(invocation, usageRecordId, candidate, providerResponse);
    }

    public static GatewayStreamingInvocation failed(GatewayInvocation invocation, UsageRecordId usageRecordId) {
        return new GatewayStreamingInvocation(invocation, usageRecordId, null, null);
    }

    public boolean opened() {
        return providerResponse != null;
    }

    public GatewayInvocation invocation() {
        return invocation;
    }

    public UsageRecordId usageRecordId() {
        return usageRecordId;
    }

    public RouteCandidate candidate() {
        return candidate;
    }

    public boolean requiresProtocolConversion() {
        return opened() && candidate.requiresProtocolConversion();
    }

    public ProviderStreamingResponse providerResponse() {
        if (providerResponse == null) {
            throw new IllegalStateException("Streaming provider response is not opened");
        }
        return providerResponse;
    }
}
