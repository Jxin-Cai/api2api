package com.api2api.ohs.http.gateway;

/** Planner rejected the bridged request; JSON and streaming clients fall back to ordinary Responses passthrough. */
final class ResponsesImageBridgePassthrough extends RuntimeException {
    private final int statusCode;

    ResponsesImageBridgePassthrough(int statusCode) {
        super("Responses image planner rejected the bridged request (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
    }

    int statusCode() {
        return statusCode;
    }
}
