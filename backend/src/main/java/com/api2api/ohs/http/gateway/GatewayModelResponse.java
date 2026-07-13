package com.api2api.ohs.http.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenAI-compatible model descriptor exposed by the gateway.
 */
public record GatewayModelResponse(
        String id,
        String object,
        long created,
        @JsonProperty("owned_by") String ownedBy
) {
}
