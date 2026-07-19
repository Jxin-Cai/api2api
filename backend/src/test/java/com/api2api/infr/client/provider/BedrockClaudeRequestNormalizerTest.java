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

    @Test
    void test_rejectsNextModelCall_when_sameReadIntentRepeatsWithDifferentFiles() {
        // Arrange
        String body = """
                {"messages":[
                  {"role":"assistant","content":[{"type":"text","text":"让我看看 _build_channel_env 返回 None 后 SDK 是如何处理的。"},{"type":"tool_use","id":"r1","name":"Read","input":{"file_path":"runtime.py"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"r1","content":"one"}]},
                  {"role":"assistant","content":[{"type":"text","text":"让我看看 _build_channel_env 返回 None 后 SDK 如何处理它，确认是否覆盖 settings。"},{"type":"tool_use","id":"r2","name":"Read","input":{"file_path":"client.py"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"r2","content":"two"}]},
                  {"role":"assistant","content":[{"type":"text","text":"让我直接看 _build_channel_env 返回 None 后 SDK 是如何处理的。"},{"type":"tool_use","id":"r3","name":"Read","input":{"file_path":"subprocess.py"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"r3","content":"three"}]}
                ]}
                """;

        // Act / Assert
        assertThatThrownBy(() -> normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()))
                .isInstanceOf(ProtocolConversionException.class)
                .hasMessageContaining("CLAUDE_REPEATED_SUCCESSFUL_TOOL_CALL: Read repeated 3 times");
    }

    @Test
    void test_addsCorrectiveInstruction_when_sameReadIntentRepeatsAcrossTwoTurns() throws Exception {
        // Arrange
        String body = """
                {"messages":[
                  {"role":"assistant","content":[{"type":"text","text":"让我看看 runtime 返回 None 后 SDK 是如何处理环境的。"},{"type":"tool_use","id":"r1","name":"Read","input":{"file_path":"runtime.py"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"r1","content":"one"}]},
                  {"role":"assistant","content":[{"type":"text","text":"让我直接看 runtime 返回 None 后 SDK 是如何处理环境的。"},{"type":"tool_use","id":"r2","name":"Read","input":{"file_path":"client.py"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"r2","content":"two"}]}
                ]}
                """;

        // Act
        BedrockClaudeRequestNormalizer.NormalizedRequest result = normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of());
        JsonNode normalized = objectMapper.readTree(result.body());

        // Assert
        assertThat(normalized.at("/messages/3/content/1/text").asText())
                .contains("tool operation already succeeded twice");
    }

    @Test
    void test_mergesConsecutiveAssistantAndUserFragments_whenToolTurnIsSplit() throws Exception {
        // Arrange
        String body = """
                {"messages":[
                  {"role":"user","content":[{"type":"text","text":"inspect"}]},
                  {"role":"assistant","content":[{"type":"thinking","thinking":"analyzing","signature":"sig"}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"t1","name":"Read","input":{"file_path":"a.java"}}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"t2","name":"Read","input":{"file_path":"b.java"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":"file a"}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"t2","content":"file b"}]}
                ]}
                """;

        // Act
        JsonNode normalized = objectMapper.readTree(normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()).body());

        // Assert
        JsonNode messages = normalized.path("messages");
        assertThat(messages).hasSize(3);
        assertThat(messages.at("/1/role").asText()).isEqualTo("assistant");
        assertThat(messages.at("/1/content")).hasSize(3);
        assertThat(messages.at("/2/role").asText()).isEqualTo("user");
        assertThat(messages.at("/2/content")).hasSize(2);
    }

    @Test
    void test_preservesAlternatingMessageTurns_whenRolesAreAlreadyCanonical() throws Exception {
        // Arrange
        String body = """
                {"messages":[
                  {"role":"user","content":"question"},
                  {"role":"assistant","content":"answer"},
                  {"role":"user","content":"follow-up"}
                ]}
                """;

        // Act
        JsonNode normalized = objectMapper.readTree(normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()).body());

        // Assert
        assertThat(normalized.path("messages")).hasSize(3);
        assertThat(normalized.at("/0/content/0/type").asText()).isEqualTo("text");
        assertThat(normalized.at("/1/content/0/text").asText()).isEqualTo("answer");
    }

    @Test
    void test_injectsToolRequirement_whenThinkingForcesToolChoiceToAuto() throws Exception {
        // Arrange
        String body = """
                {
                  "thinking":{"type":"adaptive"},
                  "tool_choice":{"type":"any"},
                  "system":"You are helpful.",
                  "messages":[{"role":"user","content":"continue"}]
                }
                """;

        // Act
        JsonNode normalized = objectMapper.readTree(normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()).body());

        // Assert
        assertThat(normalized.at("/tool_choice/type").asText()).isEqualTo("auto");
        assertThat(normalized.path("system").asText())
                .contains("MUST use at least one tool");
    }

    @Test
    void test_doesNotInjectToolRequirement_whenToolChoiceIsAlreadyAutomatic() throws Exception {
        // Arrange
        String body = """
                {
                  "thinking":{"type":"adaptive"},
                  "tool_choice":{"type":"auto"},
                  "system":"You are helpful.",
                  "messages":[{"role":"user","content":"continue"}]
                }
                """;

        // Act
        JsonNode normalized = objectMapper.readTree(normalizer.normalize(
                body, "anthropic.claude-opus-4-6-v1", Map.of()).body());

        // Assert
        assertThat(normalized.path("system").asText()).isEqualTo("You are helpful.");
    }
}
