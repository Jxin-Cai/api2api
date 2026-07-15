package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClaudeConversationContextOptimizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test_clearsOlderThinking_when_keepLastThinkingTurnConfigured() throws Exception {
        // Arrange
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"old","signature":"old-sig"},
                    {"type":"text","text":"old answer"}
                  ]},
                  {"role":"user","content":"continue"},
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"current","signature":"current-sig"},
                    {"type":"tool_use","id":"call-1","name":"Read","input":{"file_path":"a"}}
                  ]}
                ]
                """);
        JsonNode policy = objectMapper.readTree("""
                {"edits":[{"type":"clear_thinking_20251015","keep":{"type":"thinking_turns","value":1}}]}
                """);

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, policy);

        // Assert
        assertThat(optimized.at("/0/content").findValuesAsText("type")).containsExactly("text");
        assertThat(optimized.at("/2/content").findValuesAsText("type")).containsExactly("thinking", "tool_use");
    }

    @Test
    void test_clearsAllThinking_when_compactionIsRequested() throws Exception {
        // Arrange
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"provider state","signature":"opaque-signature"},
                    {"type":"text","text":"visible answer"}
                  ]},
                  {"role":"assistant","content":[
                    {"type":"redacted_thinking","data":"opaque-redacted-state"}
                  ]},
                  {"role":"user","content":"continue"}
                ]
                """);
        JsonNode policy = objectMapper.readTree("""
                {"edits":[{"type":"compact_20260112","trigger":{"value":80000}}]}
                """);

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, policy);

        // Assert
        assertThat(optimized.toString()).doesNotContain("opaque-signature", "opaque-redacted-state");
        assertThat(optimized.at("/0/content/0/text").asText()).isEqualTo("visible answer");
        assertThat(optimized.at("/1/content/0/text").asText())
                .isEqualTo("[Thinking cleared by context management]");
    }

    @Test
    void test_clearsOldToolResult_when_toolUseThresholdExceeded() throws Exception {
        // Arrange
        JsonNode messages = toolHistory("Read", "Read");
        JsonNode policy = objectMapper.readTree("""
                {"edits":[{
                  "type":"clear_tool_uses_20250919",
                  "trigger":{"type":"tool_uses","value":1},
                  "keep":{"type":"tool_uses","value":1}
                }]}
                """);

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, policy);

        // Assert
        assertThat(optimized.at("/1/content/0/content").asText())
                .isEqualTo("[Tool result cleared by context management]");
        assertThat(optimized.at("/3/content/0/content").asText()).isEqualTo("result-2");
    }

    @Test
    void test_preservesExcludedToolResult_when_contextClearingRuns() throws Exception {
        // Arrange
        JsonNode messages = toolHistory("Memory", "Read");
        JsonNode policy = objectMapper.readTree("""
                {"edits":[{
                  "type":"clear_tool_uses_20250919",
                  "trigger":{"type":"tool_uses","value":1},
                  "keep":{"type":"tool_uses","value":0},
                  "exclude_tools":["Memory"]
                }]}
                """);

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, policy);

        // Assert
        assertThat(optimized.at("/1/content/0/content").asText()).isEqualTo("result-1");
        assertThat(optimized.at("/3/content/0/content").asText())
                .isEqualTo("[Tool result cleared by context management]");
    }

    @Test
    void test_rejectsNextModelCall_when_sameSuccessfulToolCallRepeatedThreeTimes() throws Exception {
        // Arrange
        JsonNode messages = repeatedToolHistory(3);

        // Act / Assert
        assertThatThrownBy(() -> ClaudeConversationContextOptimizer.optimize(messages, null))
                .isInstanceOf(ProtocolConversionException.class)
                .hasMessageContaining("CLAUDE_REPEATED_SUCCESSFUL_TOOL_CALL: Write repeated 3 times");
    }

    @Test
    void test_addsCorrectiveInstruction_when_sameSuccessfulToolCallRepeatedTwice() throws Exception {
        // Arrange
        JsonNode messages = repeatedToolHistory(2);

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, null);

        // Assert
        assertThat(optimized.at("/3/content/1/text").asText())
                .contains("tool operation already succeeded twice")
                .contains("execute the next distinct action");
    }

    @Test
    void test_rejectsNextModelCall_when_bashCommandRepeatsWithDifferentDescriptions() throws Exception {
        // Arrange
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-1","name":"Bash","input":{"command":"git status --short","description":"Check status"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-1","content":""}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-2","name":"Bash","input":{"command":"git status --short","description":"Verify clean state"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-2","content":""}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-3","name":"Bash","input":{"command":"git status --short","description":"Check current status before push"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-3","content":""}]}
                ]
                """);

        // Act / Assert
        assertThatThrownBy(() -> ClaudeConversationContextOptimizer.optimize(messages, null))
                .isInstanceOf(ProtocolConversionException.class)
                .hasMessageContaining("CLAUDE_REPEATED_SUCCESSFUL_TOOL_CALL: Bash repeated 3 times");
    }

    @Test
    void test_clearsOldToolResultByDefault_when_contextExceedsSafetyThreshold() throws Exception {
        // Arrange
        String largeResult = "x".repeat(410_000);
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-1","name":"Read","input":{"path":"1"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-1","content":"%s"}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-2","name":"Read","input":{"path":"2"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-2","content":"result-2"}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-3","name":"Read","input":{"path":"3"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-3","content":"result-3"}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-4","name":"Read","input":{"path":"4"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-4","content":"result-4"}]}
                ]
                """.formatted(largeResult));

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, null);

        // Assert
        assertThat(optimized.at("/1/content/0/content").asText())
                .isEqualTo("[Tool result cleared by context management]");
        assertThat(optimized.at("/7/content/0/content").asText()).isEqualTo("result-4");
    }

    @Test
    void test_allowsModelCall_when_successfulToolCallsDiffer() throws Exception {
        // Arrange
        JsonNode messages = toolHistory("Read", "Write");

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, null);

        // Assert
        assertThat(optimized).isSameAs(messages);
    }

    @Test
    void test_resetsRepeatedCallCounter_when_newUserInstructionStarts() throws Exception {
        // Arrange
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-1","name":"Write","input":{"path":"a"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-1","content":"success"}]},
                  {"role":"user","content":"Write the file again intentionally"},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-2","name":"Write","input":{"path":"a"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-2","content":"success"}]},
                  {"role":"user","content":"Write the file one final time"},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-3","name":"Write","input":{"path":"a"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-3","content":"success"}]}
                ]
                """);

        // Act
        JsonNode optimized = ClaudeConversationContextOptimizer.optimize(messages, null);

        // Assert
        assertThat(optimized).isSameAs(messages);
    }

    private JsonNode toolHistory(String firstTool, String secondTool) throws Exception {
        return objectMapper.readTree("""
                [
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-1","name":"%s","input":{"path":"a"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-1","content":"result-1"}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"call-2","name":"%s","input":{"path":"b"}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-2","content":"result-2"}]}
                ]
                """.formatted(firstTool, secondTool));
    }

    private JsonNode repeatedToolHistory(int repetitions) throws Exception {
        StringBuilder messages = new StringBuilder("[");
        for (int index = 1; index <= repetitions; index++) {
            if (index > 1) {
                messages.append(',');
            }
            messages.append("""
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-%1$d","name":"Write","input":{"path":"a","content":"same"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-%1$d","content":"success"}]}
                    """.formatted(index));
        }
        return objectMapper.readTree(messages.append(']').toString());
    }
}
