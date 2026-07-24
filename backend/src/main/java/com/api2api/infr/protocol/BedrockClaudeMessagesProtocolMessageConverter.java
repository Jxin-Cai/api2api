package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class BedrockClaudeMessagesProtocolMessageConverter extends AbstractProtocolMessageConverter {

    private static final String BEDROCK_ANTHROPIC_VERSION = "bedrock-2023-05-31";

    BedrockClaudeMessagesProtocolMessageConverter(
            ProtocolJsonSupport json,
            UnifiedUsageExtractor usageExtractor,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionDirection direction,
            SseEventTransformer sseEventTransformer
    ) {
        super(json, usageExtractor, sourceProtocol, targetProtocol, direction, sseEventTransformer);
    }

    @Override
    protected JsonNode convertRequestJson(JsonNode source, ProtocolConversionRequest requirement) {
        ObjectNode target = source.deepCopy();
        target.remove("model");
        target.remove("stream");
        target.put("anthropic_version", BEDROCK_ANTHROPIC_VERSION);

        JsonNode messages = target.get("messages");
        JsonNode protectedMessages = ClaudeConversationContextOptimizer.protectAgainstRepeatedToolCalls(messages);
        if (protectedMessages != messages) {
            target.set("messages", protectedMessages);
        }

        return target;
    }

    @Override
    protected JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement) {
        return source.deepCopy();
    }
}
