package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BedrockClaudeRequestNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BedrockClaudeRequestNormalizer normalizer = new BedrockClaudeRequestNormalizer(objectMapper);

    @Test
    void test_preservesLatestToolResult_when_normalizingOpus46LongConversation() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "stream":true,
                  "max_tokens":16000,
                  "thinking":{"type":"adaptive"},
                  "messages":[
                    {"role":"user","content":"continue"},
                    {"role":"assistant","content":[
                      {"type":"thinking","thinking":"check state","signature":"signed"},
                      {"type":"tool_use","id":"tooluse_1","name":"Bash","input":{"command":"git status"}}
                    ]},
                    {"role":"user","content":[
                      {"type":"tool_result","tool_use_id":"tooluse_1","is_error":false,"content":"clean"}
                    ]}
                  ]
                }
                """;

        // Act
        BedrockClaudeRequestNormalizer.NormalizedRequest result = normalizer.normalize(
                body, "us.anthropic.claude-opus-4-6-v1", Map.of());
        JsonNode normalized = objectMapper.readTree(result.body());

        // Assert
        assertThat(normalized.at("/messages/2/content/0/tool_use_id").asText()).isEqualTo("tooluse_1");
        assertThat(result.diagnostics().unmatchedToolResultCount()).isZero();
    }

    @Test
    void test_keepsCompactionAndRemovesToolClearing_when_modelIsOpus46() throws Exception {
        // Arrange
        String body = """
                {
                  "messages":[{"role":"user","content":"continue"}],
                  "context_management":{"edits":[
                    {"type":"clear_tool_uses_20250919","keep":{"type":"tool_uses","value":3}},
                    {"type":"compact_20260112"}
                  ]}
                }
                """;
        Map<String, List<String>> headers = Map.of(
                "anthropic-beta",
                List.of("context-management-2025-06-27, compact-2026-01-12, task-budgets-2026-03-13")
        );

        // Act
        BedrockClaudeRequestNormalizer.NormalizedRequest result = normalizer.normalize(
                body, "global.anthropic.claude-opus-4-6-v1", headers);
        JsonNode normalized = objectMapper.readTree(result.body());

        // Assert
        assertThat(normalized.at("/context_management/edits/0/type").asText())
                .isEqualTo("compact_20260112");
        assertThat(normalized.path("context_management").path("edits")).hasSize(1);
        assertThat(normalized.path("anthropic_beta").toString())
                .isEqualTo("[\"compact-2026-01-12\"]");
    }

    @Test
    void test_mapsClaudeCodeDeferredToolAndAdvancedBeta_when_modelSupportsToolSearch() throws Exception {
        // Arrange
        String body = """
                {
                  "tools":[
                    {"type":"tool_search_tool_regex_20251119","name":"tool_search"},
                    {"name":"Bash","input_schema":{"type":"object"},"custom":{"defer_loading":true}}
                  ],
                  "messages":[{"role":"user","content":"inspect"}]
                }
                """;

        // Act
        BedrockClaudeRequestNormalizer.NormalizedRequest result = normalizer.normalize(
                body,
                "anthropic.claude-opus-4-6-v1",
                Map.of("anthropic-beta", List.of("advanced-tool-use-2025-11-20"))
        );
        JsonNode normalized = objectMapper.readTree(result.body());

        // Assert
        assertThat(normalized.at("/tools/1/custom").isMissingNode()).isTrue();
        assertThat(normalized.at("/tools/1/defer_loading").asBoolean()).isTrue();
        assertThat(normalized.path("anthropic_beta").toString())
                .isEqualTo("[\"tool-search-tool-2025-10-19\"]");
    }

    @Test
    void test_removesDirectOnlyCacheFields_when_normalizingBedrockRequest() throws Exception {
        // Arrange
        String body = """
                {
                  "system":[{"type":"text","text":"stable","cache_control":{
                    "type":"ephemeral","scope":"global","ttl":"1h"
                  }}],
                  "messages":[{"role":"user","content":[{"type":"text","text":"go","cache_control":{
                    "type":"ephemeral","scope":"session","ttl":"invalid"
                  }}]}]
                }
                """;

        // Act
        JsonNode normalized = objectMapper.readTree(normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()).body());

        // Assert
        assertThat(normalized.at("/system/0/cache_control").toString())
                .isEqualTo("{\"type\":\"ephemeral\",\"ttl\":\"1h\"}");
        assertThat(normalized.at("/messages/0/content/0/cache_control").toString())
                .isEqualTo("{\"type\":\"ephemeral\"}");
    }

    @Test
    void test_usesAutomaticToolChoice_when_thinkingIsEnabled() throws Exception {
        // Arrange
        String body = """
                {
                  "thinking":{"type":"adaptive"},
                  "tool_choice":{"type":"tool","name":"Bash"},
                  "tools":[{"name":"Bash","input_schema":{"type":"object"}}],
                  "messages":[{"role":"user","content":"inspect"}]
                }
                """;

        // Act
        JsonNode normalized = objectMapper.readTree(normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()).body());

        // Assert
        assertThat(normalized.at("/tool_choice/type").asText()).isEqualTo("auto");
    }

    @Test
    void test_rejectsNextModelCall_when_sameCallAlreadyCompletedThreeTimes() {
        // Arrange
        String body = """
                {
                  "messages":[
                    {"role":"assistant","content":[{"type":"tool_use","id":"t1","name":"Bash","input":{"command":"git status"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","is_error":false,"content":"clean"}]},
                    {"role":"assistant","content":[{"type":"tool_use","id":"t2","name":"Bash","input":{"command":"git status"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"t2","is_error":false,"content":"clean"}]},
                    {"role":"assistant","content":[{"type":"tool_use","id":"t3","name":"Bash","input":{"command":"git status"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"t3","is_error":false,"content":"clean"}]}
                  ]
                }
                """;

        // Act
        // Act / Assert
        assertThatThrownBy(() -> normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()))
                .isInstanceOf(ProtocolConversionException.class)
                .hasMessageContaining("CLAUDE_REPEATED_SUCCESSFUL_TOOL_CALL");
    }

    @Test
    void test_addsCorrectiveInstruction_when_sameCallAlreadyCompletedTwice() throws Exception {
        // Arrange
        String body = """
                {
                  "messages":[
                    {"role":"assistant","content":[{"type":"tool_use","id":"t1","name":"Bash","input":{"command":"git status","description":"first"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","is_error":false,"content":"clean"}]},
                    {"role":"assistant","content":[{"type":"tool_use","id":"t2","name":"Bash","input":{"command":"git status","description":"second"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"t2","is_error":false,"content":"clean"}]}
                  ]
                }
                """;

        // Act
        JsonNode normalized = objectMapper.readTree(normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()).body());

        // Assert
        assertThat(normalized.at("/messages/3/content/1/text").asText())
                .contains("already succeeded twice");
    }
}
