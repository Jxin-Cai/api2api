package com.api2api.ohs.http.gateway;

/**
 * Internal adapter contract for protocol-compatible gateway requests.
 */
interface GatewayProtocolRequest {

    String rawBody();

    String model();

    boolean streaming();

    boolean toolCallingRequired();

    boolean reasoningRequired();
}
