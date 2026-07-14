package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClaudeRequestSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test_removesThinkingSignatures_when_claudeCompactionPassesThrough() throws Exception {
        // Arrange
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, """
                {
                  "model":"claude-opus-4-8",
                  "context_management":{"edits":[{"type":"compact_20260112"}]},
                  "messages":[
                    {"role":"assistant","content":[
                      {"type":"thinking","thinking":"summary","signature":"foreign-provider-signature"},
                      {"type":"text","text":"visible answer"}
                    ]},
                    {"role":"user","content":"summarize"}
                  ]
                }
                """, true);

        // Act
        ProtocolPayload sanitized = ClaudeRequestSanitizer.sanitize(
                objectMapper, payload, ProtocolType.CLAUDE_MESSAGES);
        JsonNode body = objectMapper.readTree(sanitized.body());

        // Assert
        assertThat(body.toString()).doesNotContain("foreign-provider-signature");
        assertThat(body.at("/messages/0/content/0/text").asText()).isEqualTo("visible answer");
    }

    @Test
    void test_removesResponsesThinkingState_when_requestPassesThroughToClaude() throws Exception {
        // Arrange
        String signature = ResponsesReasoningBridge.encode(objectMapper, objectMapper.readTree("""
                {"type":"reasoning","id":"rs_1","encrypted_content":"encrypted"}
                """)).orElseThrow();
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, """
                {"messages":[{"role":"assistant","content":[
                  {"type":"thinking","thinking":"summary","signature":"%s"},
                  {"type":"text","text":"visible answer"}
                ]}]}
                """.formatted(signature), false);

        // Act
        ProtocolPayload sanitized = ClaudeRequestSanitizer.sanitize(
                objectMapper, payload, ProtocolType.CLAUDE_MESSAGES);
        JsonNode body = objectMapper.readTree(sanitized.body());

        // Assert
        assertThat(body.at("/messages/0/content/0/type").asText()).isEqualTo("text");
        assertThat(body.at("/messages/0/content/0/text").asText()).isEqualTo("visible answer");
    }

    @Test
    void test_preservesResponsesThinkingState_when_requestTargetsResponses() throws Exception {
        // Arrange
        String signature = ResponsesReasoningBridge.encode(objectMapper, objectMapper.readTree("""
                {"type":"reasoning","id":"rs_1","encrypted_content":"encrypted"}
                """)).orElseThrow();
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, """
                {"messages":[{"role":"assistant","content":[
                  {"type":"thinking","thinking":"summary","signature":"%s"}
                ]}]}
                """.formatted(signature), false);

        // Act
        ProtocolPayload sanitized = ClaudeRequestSanitizer.sanitize(
                objectMapper, payload, ProtocolType.OPENAI_RESPONSES);

        // Assert
        assertThat(sanitized).isSameAs(payload);
    }

    @Test
    void test_preservesNativeThinkingState_when_requestPassesThroughToClaude() {
        // Arrange
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, """
                {"messages":[{"role":"assistant","content":[
                  {"type":"thinking","thinking":"summary","signature":"native-claude-signature"}
                ]}]}
                """, false);

        // Act
        ProtocolPayload sanitized = ClaudeRequestSanitizer.sanitize(
                objectMapper, payload, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(sanitized).isSameAs(payload);
    }
}
