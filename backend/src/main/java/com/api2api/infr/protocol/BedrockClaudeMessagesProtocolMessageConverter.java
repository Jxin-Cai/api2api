package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
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
    private static final String FALLBACK_CREDIT_BETA = "fallback-credit-2026-06-09";
    private static final Set<String> UNSUPPORTED_CLAUDE_PLATFORM_FIELDS = Set.of(
            "container",
            "diagnostics",
            "fallbacks",
            "inference_geo",
            "mcp_servers"
    );
    private static final Set<String> BEDROCK_SUPPORTED_BETA_FEATURES = Set.of(
            "computer-use-2024-10-22",
            "computer-use-2025-01-24",
            "computer-use-2025-11-24",
            "fine-grained-tool-streaming-2025-05-14",
            "token-efficient-tools-2025-02-19",
            "interleaved-thinking-2025-05-14",
            "output-128k-2025-02-19",
            "dev-full-thinking-2025-05-14",
            "context-1m-2025-08-07",
            CONTEXT_MANAGEMENT_BETA,
            "effort-2025-11-24",
            "tool-search-tool-2025-10-19",
            "tool-examples-2025-10-29",
            COMPACTION_BETA,
            FALLBACK_CREDIT_BETA
    );

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
        rejectUnsupportedClaudePlatformFields(source);
        ObjectNode target = source.deepCopy();
        target.remove("model");
        target.remove("stream");
        removeUnsupportedThinkingFields(target);
        BedrockWebSearchToolCompatibility.normalizeRequest(target);
        BedrockStructuredOutputCompatibility.normalizeRequest(target);
        Set<String> requiredBetaFeatures =
                BedrockClaudeMessagesRequestValidator.validateAndCollectRequiredBetaFeatures(target);
        target.put("anthropic_version", BEDROCK_ANTHROPIC_VERSION);
        mergeAnthropicBetaFeatures(target, requirement, requiredBetaFeatures);
        return target;
    }

    private void removeUnsupportedThinkingFields(ObjectNode target) {
        JsonNode thinking = target.get("thinking");
        if (thinking instanceof ObjectNode thinkingObject) {
            thinkingObject.remove("display");
        }
    }

    private void rejectUnsupportedClaudePlatformFields(JsonNode source) {
        for (String field : UNSUPPORTED_CLAUDE_PLATFORM_FIELDS) {
            if (source.hasNonNull(field)) {
                throw new ProtocolConversionException(
                        "Claude Messages field '" + field + "' has no Bedrock InvokeModel equivalent");
            }
        }
    }

    private void mergeAnthropicBetaFeatures(
            ObjectNode target,
            ProtocolConversionRequest requirement,
            Set<String> requiredBetaFeatures
    ) {
        Set<String> betaFeatures = new LinkedHashSet<>();
        JsonNode existingFeatures = target.get("anthropic_beta");
        if (existingFeatures != null && existingFeatures.isArray()) {
            existingFeatures.forEach(feature -> addBetaFeature(betaFeatures, feature.asText("")));
        }
        requirement.anthropicBetaFeatures().forEach(feature -> addBetaFeature(betaFeatures, feature));
        requiredBetaFeatures.forEach(feature -> addBetaFeature(betaFeatures, feature));
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
        if (feature == null || feature.isBlank()) {
            return;
        }
        String normalized = feature.trim();
        if (BEDROCK_SUPPORTED_BETA_FEATURES.contains(normalized)) {
            betaFeatures.add(normalized);
        }
    }

    @Override
    protected JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement) {
        JsonNode target = source.deepCopy();
        BedrockClaudeMessagesResponseSanitizer.removeProviderExtensions(target);
        BedrockStructuredOutputCompatibility.unwrapResponse(target);
        return target;
    }
}
