package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.Set;

final class BedrockClaudeMessagesProtocolMessageConverter extends AbstractProtocolMessageConverter {

    private static final String BEDROCK_ANTHROPIC_VERSION = "bedrock-2023-05-31";
    private static final String CONTEXT_MANAGEMENT_BETA = "context-management-2025-06-27";
    private static final String COMPACTION_BETA = "compact-2026-01-12";

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
        mergeAnthropicBetaFeatures(target, requirement);

        JsonNode messages = target.get("messages");
        JsonNode protectedMessages = ClaudeConversationContextOptimizer.protectAgainstRepeatedToolCalls(messages);
        if (protectedMessages != messages) {
            target.set("messages", protectedMessages);
        }

        return target;
    }

    private void mergeAnthropicBetaFeatures(ObjectNode target, ProtocolConversionRequest requirement) {
        Set<String> betaFeatures = new LinkedHashSet<>();
        JsonNode existingFeatures = target.get("anthropic_beta");
        if (existingFeatures != null && existingFeatures.isArray()) {
            existingFeatures.forEach(feature -> addBetaFeature(betaFeatures, feature.asText("")));
        }
        requirement.anthropicBetaFeatures().forEach(feature -> addBetaFeature(betaFeatures, feature));
        addContextManagementBetaFeatures(betaFeatures, target.get("context_management"));
        if (betaFeatures.isEmpty()) {
            target.remove("anthropic_beta");
            return;
        }
        ArrayNode mappedFeatures = target.arrayNode();
        betaFeatures.forEach(mappedFeatures::add);
        target.set("anthropic_beta", mappedFeatures);
    }

    private void addContextManagementBetaFeatures(Set<String> betaFeatures, JsonNode contextManagement) {
        if (contextManagement == null || contextManagement.isNull()) {
            return;
        }
        JsonNode edits = contextManagement.isArray()
                ? contextManagement
                : contextManagement.path("edits");
        if (!edits.isArray()) {
            return;
        }
        for (JsonNode edit : edits) {
            String type = edit.path("type").asText("");
            if ("compact_20260112".equals(type)) {
                addBetaFeature(betaFeatures, COMPACTION_BETA);
            } else if ("clear_tool_uses_20250919".equals(type)
                    || "clear_thinking_20251015".equals(type)) {
                addBetaFeature(betaFeatures, CONTEXT_MANAGEMENT_BETA);
            }
        }
    }

    private void addBetaFeature(Set<String> betaFeatures, String feature) {
        if (feature != null && !feature.isBlank()) {
            betaFeatures.add(feature.trim());
        }
    }

    @Override
    protected JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement) {
        return source.deepCopy();
    }
}
