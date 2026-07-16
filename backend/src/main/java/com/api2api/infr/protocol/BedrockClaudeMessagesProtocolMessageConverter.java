package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Bridges Anthropic Claude Messages to Bedrock InvokeModel without reshaping
 * message, tool, reasoning, cache, or context-management blocks.
 */
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
        if (sourceProtocol() != ProtocolType.CLAUDE_MESSAGES
                || targetProtocol() != ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES) {
            throw new IllegalStateException("Unexpected Bedrock Claude Messages request conversion");
        }
        if (source == null || !source.isObject()) {
            throw new ProtocolConversionException("BEDROCK_INVOKE_REQUEST_MUST_BE_OBJECT");
        }
        ObjectNode target = ((ObjectNode) source).deepCopy();
        target.remove("model");
        target.remove("stream");
        target.put("anthropic_version", BEDROCK_ANTHROPIC_VERSION);
        return target;
    }

    @Override
    protected JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement) {
        if (sourceProtocol() != ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES
                || targetProtocol() != ProtocolType.CLAUDE_MESSAGES) {
            throw new IllegalStateException("Unexpected Bedrock Claude Messages response conversion");
        }
        if (source == null || !source.isObject()) {
            throw new ProtocolConversionException("BEDROCK_INVOKE_RESPONSE_MUST_BE_OBJECT");
        }
        return source.deepCopy();
    }
}
