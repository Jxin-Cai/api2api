package com.api2api.ohs.http.gateway;

import java.util.List;

/**
 * OpenAI-compatible model list response.
 */
public record GatewayModelListResponse(String object, List<GatewayModelResponse> data) {

    public GatewayModelListResponse {
        data = List.copyOf(data);
    }
}
