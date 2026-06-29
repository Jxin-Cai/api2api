package com.api2api.domain.gateway.model;

/**
 * Lifecycle state of one gateway invocation.
 */
public enum GatewayInvocationState {
    CREATED,
    AUTHENTICATED,
    ROUTED,
    CONVERTING,
    FORWARDING,
    SUCCEEDED,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
