package com.api2api.infr.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/** Shared terminal precedence for buffered and streaming Messages responses. */
final class ResponsesStopReason {
    private ResponsesStopReason() {}

    static String resolve(JsonNode response, boolean toolCall, boolean refusal, boolean unfinishedTurn) {
        if ("incomplete".equals(response.path("status").asText())
                && "max_output_tokens".equals(response.path("incomplete_details").path("reason").asText())) {
            return "max_tokens";
        }
        if (refusal || "content_filter".equals(response.path("incomplete_details").path("reason").asText())) {
            return "refusal";
        }
        if (toolCall) {
            return "tool_use";
        }
        return unfinishedTurn ? "pause_turn" : "end_turn";
    }
}
