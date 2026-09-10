package com.api2api.ohs.http.gateway;

import org.springframework.http.MediaType;

/** An expected upstream failure, retaining the status and retry headers for JSON clients. */
final class ResponsesImageBridgeFailure extends RuntimeException {
    private final GatewayRawResponse response;

    ResponsesImageBridgeFailure(GatewayRawResponse response) {
        super("Responses image bridge upstream failure (HTTP " + response.statusCode() + ")");
        this.response = response;
    }

    GatewayRawResponse response() { return response; }

    static ResponsesImageBridgeFailure invalidUpstream(String message) {
        var error = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        error.putObject("error").put("type", "api_error").put("code", "invalid_upstream_response").put("message", message);
        return new ResponsesImageBridgeFailure(GatewayRawResponse.of(error.toString(), 502, MediaType.APPLICATION_JSON));
    }
}
