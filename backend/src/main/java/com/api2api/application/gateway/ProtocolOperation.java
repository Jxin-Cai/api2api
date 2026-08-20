package com.api2api.application.gateway;

/**
 * Protocol-level operation requested by the client.
 *
 * <p>Each operation maps onto a suffix appended to the protocol's canonical upstream path so that
 * auxiliary endpoints stay on the same routing, authentication and quota pipeline as invocations.</p>
 */
public enum ProtocolOperation {

    INVOKE(""),
    COUNT_TOKENS("/count_tokens");

    private final String upstreamPathSuffix;

    ProtocolOperation(String upstreamPathSuffix) {
        this.upstreamPathSuffix = upstreamPathSuffix;
    }

    public String upstreamPathSuffix() {
        return upstreamPathSuffix;
    }

    /**
     * Token counting and other auxiliary endpoints are not model completions. Reserving quota for
     * them would let a client that probes context size starve concurrent real invocations.
     */
    public boolean billable() {
        return this == INVOKE;
    }

    public boolean supportsStreaming() {
        return this == INVOKE;
    }

    /**
     * Auxiliary endpoints exist only on the protocol that defined them. Routing them through a
     * converter would POST a transformed body to a path the target protocol does not implement.
     */
    public boolean requiresNativeProtocol() {
        return this != INVOKE;
    }
}
