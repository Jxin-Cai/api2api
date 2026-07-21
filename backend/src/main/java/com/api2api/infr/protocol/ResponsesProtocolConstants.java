package com.api2api.infr.protocol;

/**
 * Shared constants and utilities for OpenAI Responses protocol compaction handling.
 * Used by both GenericProtocolMessageConverter and UnifiedStreamingConversionAdapter.
 */
final class ResponsesProtocolConstants {

    static final String OPAQUE_STATE_PLACEHOLDER = "Thinking...";
    static final String COMPACTION_PLACEHOLDER = "Context compacted.";
    static final String COMPACTION_VISIBLE_TEXT = "Conversation compacted.";

    static boolean isCompactionType(String type) {
        return "compaction".equals(type) || "compaction_summary".equals(type);
    }

    private ResponsesProtocolConstants() {
    }
}
