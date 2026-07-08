package com.api2api.application.gateway;

import com.api2api.domain.gateway.model.GatewayInvocation;
import java.util.Objects;

/**
 * Application result of a non-streaming gateway invocation, including raw upstream response when available.
 */
public final class GatewayInvocationOutcome {

    private final GatewayInvocation invocation;
    private final ProviderGatewayResponse providerResponse;

    private GatewayInvocationOutcome(GatewayInvocation invocation, ProviderGatewayResponse providerResponse) {
        this.invocation = Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        this.providerResponse = providerResponse;
    }

    public static GatewayInvocationOutcome of(GatewayInvocation invocation, ProviderGatewayResponse providerResponse) {
        return new GatewayInvocationOutcome(invocation, providerResponse);
    }

    public static GatewayInvocationOutcome withoutProviderResponse(GatewayInvocation invocation) {
        return new GatewayInvocationOutcome(invocation, null);
    }

    public boolean hasProviderResponse() {
        return providerResponse != null;
    }

    public GatewayInvocation invocation() {
        return invocation;
    }

    public ProviderGatewayResponse providerResponse() {
        if (providerResponse == null) {
            throw new IllegalStateException("Provider response is not available");
        }
        return providerResponse;
    }
}
