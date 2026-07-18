package com.api2api.infr.client.provider;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.infr.protocol.ClaudeConversationContextOptimizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes Anthropic direct-API requests for Bedrock InvokeModel.
 *
 * <p>Claude Code's direct Anthropic and Bedrock transports deliberately send
 * different feature sets. Treating the request as lossless passthrough leaves
 * direct-only beta flags and fields active on Bedrock and can break tool-result
 * continuation in long-running sessions.</p>
 */
final class BedrockClaudeRequestNormalizer {

    private static final String BEDROCK_ANTHROPIC_VERSION = "bedrock-2023-05-31";
    private static final String BETA_COMPACTION = "compact-2026-01-12";
    private static final String BETA_CONTEXT_MANAGEMENT = "context-management-2025-06-27";
    private static final String BETA_LONG_CONTEXT = "context-1m-2025-08-07";
    private static final String BETA_TOOL_SEARCH = "tool-search-tool-2025-10-19";
    private static final String BETA_TOOL_EXAMPLES = "tool-examples-2025-10-29";

    /** AWS-documented InvokeModel beta flags plus current Claude Code Bedrock flags. */
    private static final Set<String> BEDROCK_BETA_FLAGS = Set.of(
            "computer-use-2025-01-24",
            "computer-use-2025-11-24",
            "token-efficient-tools-2025-02-19",
            "interleaved-thinking-2025-05-14",
            "output-128k-2025-02-19",
            "dev-full-thinking-2025-05-14",
            BETA_LONG_CONTEXT,
            BETA_CONTEXT_MANAGEMENT,
            "effort-2025-11-24",
            BETA_TOOL_SEARCH,
            BETA_TOOL_EXAMPLES,
            BETA_COMPACTION,
            "fine-grained-tool-streaming-2025-05-14",
            "extended-cache-ttl-2025-04-11"
    );

    private static final Map<String, String> BEDROCK_BETA_ALIASES = Map.of(
            "advanced-tool-use-2025-11-20", BETA_TOOL_SEARCH,
            "interleaved-thinking-2025-05-14", "interleaved-thinking-2025-05-14"
    );

    private final ObjectMapper objectMapper;

    BedrockClaudeRequestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    NormalizedRequest normalize(String body, String upstreamModel, Map<String, List<String>> incomingHeaders) {
        try {
            JsonNode parsed = objectMapper.readTree(body);
            if (parsed == null || !parsed.isObject()) {
                throw new ProtocolConversionException("BEDROCK_INVOKE_REQUEST_MUST_BE_OBJECT");
            }
            ObjectNode request = ((ObjectNode) parsed).deepCopy();
            ModelCapabilities model = ModelCapabilities.from(upstreamModel);
            MutableDiagnostics diagnostics = new MutableDiagnostics();

            request.remove(List.of("model", "stream", "provider"));
            request.put("anthropic_version", BEDROCK_ANTHROPIC_VERSION);

            normalizeTools(request, model, diagnostics);
            normalizeThinking(request, model, diagnostics);
            normalizeCacheControl(request, model, diagnostics);
            normalizeContextManagement(request, model, diagnostics);
            normalizeBetaFlags(request, incomingHeaders, model, diagnostics);
            inspectToolContinuation(request.path("messages"), diagnostics);
            JsonNode protectedMessages = ClaudeConversationContextOptimizer.protectAgainstRepeatedToolCalls(
                    request.path("messages"));
            if (protectedMessages != request.path("messages")) {
                request.set("messages", protectedMessages);
                diagnostics.changed("tool_loop.corrective_instruction");
            }

            return new NormalizedRequest(
                    objectMapper.writeValueAsString(request),
                    diagnostics.freeze(request.path("anthropic_beta"))
            );
        } catch (ProtocolConversionException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new ProtocolConversionException("BEDROCK_INVOKE_REQUEST_PREPARATION_FAILED", exception);
        }
    }

    private void normalizeTools(ObjectNode request, ModelCapabilities model, MutableDiagnostics diagnostics) {
        JsonNode tools = request.get("tools");
        if (tools == null || !tools.isArray()) {
            return;
        }
        for (JsonNode value : tools) {
            if (!value.isObject()) {
                continue;
            }
            ObjectNode tool = (ObjectNode) value;
            JsonNode custom = tool.remove("custom");
            if (custom != null) {
                diagnostics.changed("tool.custom");
                if (!tool.has("defer_loading") && custom.path("defer_loading").isBoolean()) {
                    tool.set("defer_loading", custom.get("defer_loading"));
                }
            }
            if (!model.supportsToolSearch() && tool.remove("defer_loading") != null) {
                diagnostics.changed("tool.defer_loading");
            }
        }
    }

    private void normalizeThinking(ObjectNode request, ModelCapabilities model, MutableDiagnostics diagnostics) {
        JsonNode thinkingNode = request.get("thinking");
        if (thinkingNode != null && thinkingNode.isObject()) {
            ObjectNode thinking = (ObjectNode) thinkingNode;
            if (model.requiresAdaptiveThinking() && "enabled".equals(thinking.path("type").asText())) {
                thinking.put("type", "adaptive");
                thinking.remove("budget_tokens");
                diagnostics.changed("thinking.enabled_to_adaptive");
            }
            JsonNode toolChoiceNode = request.get("tool_choice");
            if (toolChoiceNode != null && toolChoiceNode.isObject()) {
                String choice = toolChoiceNode.path("type").asText("");
                if ("any".equals(choice) || "tool".equals(choice)) {
                    ObjectNode automatic = objectMapper.createObjectNode();
                    automatic.put("type", "auto");
                    request.set("tool_choice", automatic);
                    diagnostics.changed("thinking.forced_tool_choice");
                }
            }
        }
    }

    private void normalizeCacheControl(ObjectNode request, ModelCapabilities model, MutableDiagnostics diagnostics) {
        visitObjects(request, object -> {
            JsonNode cacheNode = object.get("cache_control");
            if (cacheNode == null || !cacheNode.isObject()) {
                return;
            }
            ObjectNode cache = (ObjectNode) cacheNode;
            if (cache.remove("scope") != null) {
                diagnostics.changed("cache_control.scope");
            }
            JsonNode ttl = cache.get("ttl");
            if (ttl != null && (!model.supportsExtendedCacheTtl()
                    || !("5m".equals(ttl.asText()) || "1h".equals(ttl.asText())))) {
                cache.remove("ttl");
                diagnostics.changed("cache_control.ttl");
            }
        });
    }

    private void normalizeContextManagement(
            ObjectNode request,
            ModelCapabilities model,
            MutableDiagnostics diagnostics
    ) {
        JsonNode contextNode = request.get("context_management");
        if (contextNode == null || !contextNode.isObject()) {
            return;
        }
        JsonNode editsNode = contextNode.get("edits");
        if (editsNode == null || !editsNode.isArray()) {
            request.remove("context_management");
            diagnostics.changed("context_management.invalid");
            return;
        }
        ArrayNode retained = objectMapper.createArrayNode();
        for (JsonNode edit : editsNode) {
            String type = edit.path("type").asText("");
            boolean supported = type.startsWith("compact_")
                    ? model.supportsCompaction()
                    : (type.startsWith("clear_tool_uses") || type.startsWith("clear_thinking"))
                    && model.supportsContextClearing();
            if (supported) {
                retained.add(edit);
            } else {
                diagnostics.changed("context_management." + (type.isBlank() ? "unknown" : type));
            }
        }
        if (retained.isEmpty()) {
            request.remove("context_management");
        } else {
            ((ObjectNode) contextNode).set("edits", retained);
        }
    }

    private void normalizeBetaFlags(
            ObjectNode request,
            Map<String, List<String>> incomingHeaders,
            ModelCapabilities model,
            MutableDiagnostics diagnostics
    ) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        JsonNode bodyFlags = request.get("anthropic_beta");
        if (bodyFlags != null && bodyFlags.isArray()) {
            bodyFlags.forEach(flag -> addBeta(candidates, flag.asText("")));
        }
        if (incomingHeaders != null) {
            incomingHeaders.forEach((name, values) -> {
                if (name == null || !"anthropic-beta".equalsIgnoreCase(name) || values == null) {
                    return;
                }
                values.forEach(value -> {
                    if (value != null) {
                        for (String flag : value.split(",")) {
                            addBeta(candidates, flag);
                        }
                    }
                });
            });
        }

        boolean hasCompaction = hasContextEdit(request, "compact_");
        boolean hasClearing = hasContextEdit(request, "clear_");
        boolean hasToolSearch = hasToolSearch(request.path("tools"));
        if (hasCompaction) {
            candidates.add(BETA_COMPACTION);
        }
        if (hasClearing) {
            candidates.add(BETA_CONTEXT_MANAGEMENT);
        }
        if (hasToolSearch && model.supportsToolSearch()) {
            candidates.add(BETA_TOOL_SEARCH);
        }
        if (hasInputExamples(request.path("tools")) && model.supportsToolSearch()) {
            candidates.add(BETA_TOOL_EXAMPLES);
        }

        ArrayNode retained = objectMapper.createArrayNode();
        for (String candidate : candidates) {
            String normalized = BEDROCK_BETA_ALIASES.getOrDefault(candidate, candidate);
            if (BEDROCK_BETA_FLAGS.contains(normalized) && model.supportsBeta(normalized)) {
                if (!contains(retained, normalized)) {
                    retained.add(normalized);
                }
            } else {
                diagnostics.changed("beta." + candidate);
            }
        }
        if (retained.isEmpty()) {
            request.remove("anthropic_beta");
        } else {
            request.set("anthropic_beta", retained);
        }
    }

    private void inspectToolContinuation(JsonNode messages, MutableDiagnostics diagnostics) {
        if (!messages.isArray()) {
            return;
        }
        Set<String> toolUseIds = new LinkedHashSet<>();
        Set<String> toolResultIds = new LinkedHashSet<>();
        Map<String, String> toolFingerprints = new java.util.LinkedHashMap<>();
        String previousSuccessfulFingerprint = null;
        int currentRepeatedStreak = 0;
        int maximumRepeatedStreak = 0;
        for (JsonNode message : messages) {
            JsonNode content = message.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                if ("tool_use".equals(block.path("type").asText())) {
                    String id = block.path("id").asText("");
                    if (!id.isBlank()) {
                        toolUseIds.add(id);
                        toolFingerprints.put(id, toolFingerprint(block));
                    }
                } else if ("tool_result".equals(block.path("type").asText())) {
                    String id = block.path("tool_use_id").asText("");
                    if (!id.isBlank()) {
                        toolResultIds.add(id);
                    }
                    if (!block.path("is_error").asBoolean(false) && toolFingerprints.containsKey(id)) {
                        String fingerprint = toolFingerprints.get(id);
                        currentRepeatedStreak = fingerprint.equals(previousSuccessfulFingerprint)
                                ? currentRepeatedStreak + 1 : 1;
                        maximumRepeatedStreak = Math.max(maximumRepeatedStreak, currentRepeatedStreak);
                        previousSuccessfulFingerprint = fingerprint;
                    }
                }
            }
        }
        diagnostics.toolUseCount = toolUseIds.size();
        diagnostics.toolResultCount = toolResultIds.size();
        diagnostics.unmatchedToolResultCount = (int) toolResultIds.stream()
                .filter(id -> !toolUseIds.contains(id))
                .count();
        diagnostics.repeatedSuccessfulToolCallStreak = maximumRepeatedStreak;
    }

    private String toolFingerprint(JsonNode toolUse) {
        String toolName = toolUse.path("name").asText("");
        JsonNode input = toolUse.path("input");
        if ("Bash".equals(toolName) && input.isObject()) {
            ObjectNode executionInput = ((ObjectNode) input).deepCopy();
            executionInput.remove("description");
            input = executionInput;
        }
        return toolName + ":" + input;
    }

    private void visitObjects(JsonNode node, java.util.function.Consumer<ObjectNode> visitor) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            visitor.accept(object);
            Iterator<JsonNode> children = object.elements();
            while (children.hasNext()) {
                visitObjects(children.next(), visitor);
            }
        } else if (node.isArray()) {
            node.forEach(child -> visitObjects(child, visitor));
        }
    }

    private void addBeta(Set<String> target, String rawFlag) {
        String flag = rawFlag == null ? "" : rawFlag.trim().toLowerCase(Locale.ROOT);
        if (!flag.isBlank()) {
            target.add(flag);
        }
    }

    private boolean hasContextEdit(ObjectNode request, String prefix) {
        JsonNode edits = request.path("context_management").path("edits");
        if (!edits.isArray()) {
            return false;
        }
        for (JsonNode edit : edits) {
            if (edit.path("type").asText("").startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasToolSearch(JsonNode tools) {
        if (!tools.isArray()) {
            return false;
        }
        for (JsonNode tool : tools) {
            if (tool.path("type").asText("").startsWith("tool_search_tool_")
                    || tool.path("defer_loading").asBoolean(false)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInputExamples(JsonNode tools) {
        if (!tools.isArray()) {
            return false;
        }
        for (JsonNode tool : tools) {
            if (tool.path("input_examples").isArray() && !tool.path("input_examples").isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(ArrayNode values, String expected) {
        for (JsonNode value : values) {
            if (expected.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    record NormalizedRequest(String body, Diagnostics diagnostics) {
    }

    record Diagnostics(
            List<String> changes,
            List<String> betaFlags,
            int toolUseCount,
            int toolResultCount,
            int unmatchedToolResultCount,
            int repeatedSuccessfulToolCallStreak
    ) {
        boolean changed() {
            return !changes.isEmpty();
        }
    }

    private static final class MutableDiagnostics {
        private final Set<String> changes = new LinkedHashSet<>();
        private int toolUseCount;
        private int toolResultCount;
        private int unmatchedToolResultCount;
        private int repeatedSuccessfulToolCallStreak;

        private void changed(String field) {
            changes.add(field);
        }

        private Diagnostics freeze(JsonNode betaFlags) {
            List<String> flags = new ArrayList<>();
            if (betaFlags.isArray()) {
                betaFlags.forEach(flag -> flags.add(flag.asText()));
            }
            return new Diagnostics(
                    List.copyOf(changes),
                    List.copyOf(flags),
                    toolUseCount,
                    toolResultCount,
                    unmatchedToolResultCount,
                    repeatedSuccessfulToolCallStreak
            );
        }
    }

    private record ModelCapabilities(String family, int major, int minor) {

        private static ModelCapabilities from(String modelId) {
            String normalized = modelId == null ? "" : modelId.toLowerCase(Locale.ROOT)
                    .replace('.', '-');
            String family = normalized.contains("haiku") ? "haiku"
                    : normalized.contains("sonnet") ? "sonnet"
                    : normalized.contains("opus") ? "opus" : "unknown";
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?:haiku|sonnet|opus)-(\\d+)-(\\d+)")
                    .matcher(normalized);
            if (!matcher.find()) {
                return new ModelCapabilities(family, 0, 0);
            }
            return new ModelCapabilities(
                    family,
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            );
        }

        private boolean atLeast(int expectedMajor, int expectedMinor) {
            return major > expectedMajor || (major == expectedMajor && minor >= expectedMinor);
        }

        private boolean unknown() {
            return major == 0;
        }

        private boolean supportsToolSearch() {
            return unknown() || (!"haiku".equals(family) && atLeast(4, 5));
        }

        private boolean supportsCompaction() {
            return unknown() || (atLeast(4, 6) && ("opus".equals(family) || "sonnet".equals(family)));
        }

        private boolean supportsContextClearing() {
            return unknown() || (major == 4 && minor == 5);
        }

        private boolean supportsExtendedCacheTtl() {
            return unknown() || atLeast(4, 5);
        }

        private boolean requiresAdaptiveThinking() {
            return atLeast(4, 7);
        }

        private boolean supportsBeta(String beta) {
            return switch (beta) {
                case BETA_COMPACTION -> supportsCompaction();
                case BETA_CONTEXT_MANAGEMENT -> supportsContextClearing();
                case BETA_LONG_CONTEXT -> ("sonnet".equals(family) && (major == 4 || atLeast(4, 6)))
                        || ("opus".equals(family) && atLeast(4, 6));
                case BETA_TOOL_SEARCH, BETA_TOOL_EXAMPLES -> supportsToolSearch();
                case "effort-2025-11-24" -> "opus".equals(family) && atLeast(4, 5);
                default -> true;
            };
        }
    }
}
