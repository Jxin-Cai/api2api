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
        ProtocolPayload sanitized = ClaudeRequestSanitizer.sanitize(objectMapper, payload);
        JsonNode body = objectMapper.readTree(sanitized.body());

        // Assert
        assertThat(body.toString()).doesNotContain("foreign-provider-signature");
        assertThat(body.at("/messages/0/content/0/text").asText()).isEqualTo("visible answer");
    }

    @Test
    void test_preservesThinkingSignatures_when_requestIsNotCompaction() {
        // Arrange
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, """
                {"messages":[{"role":"assistant","content":[
                  {"type":"thinking","thinking":"summary","signature":"valid-current-provider-signature"}
                ]}]}
                """, false);

        // Act
        ProtocolPayload sanitized = ClaudeRequestSanitizer.sanitize(objectMapper, payload);

        // Assert
        assertThat(sanitized).isSameAs(payload);
    }
}
