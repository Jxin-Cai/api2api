package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class BedrockClaudeMessagesProtocolMessageConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BedrockClaudeMessagesProtocolMessageConverter converter =
            new BedrockClaudeMessagesProtocolMessageConverter(
                    new ProtocolJsonSupport(objectMapper),
                    null,
                    ProtocolType.CLAUDE_MESSAGES,
                    ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                    ProtocolConversionDirection.REQUEST,
                    new SseEventTransformer()
            );

    @Test
    void test_mapsAnthropicBetaHeaderFeaturesToBody_when_convertingForBedrockInvokeModel() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "anthropic_beta":["existing-beta"],
                  "messages":[{"role":"user","content":"hello"}]
                }
                """;
        ProtocolConversionRequest requirement = ConversionRequirement.of(true, false, false)
                .withAnthropicBetaFeatures(List.of(
                        "context-management-2025-06-27",
                        "existing-beta"
                ))
                .forRoute(1783929967772706L, "claude-opus-4.6")
                .toProtocolConversionRequest();

        // Act
        JsonNode mapped = convert(body, requirement);

        // Assert
        assertThat(mapped.path("anthropic_beta"))
                .containsExactly(
                        objectMapper.getNodeFactory().textNode("existing-beta"),
                        objectMapper.getNodeFactory().textNode("context-management-2025-06-27")
                );
    }

    @Test
    void test_addsContextManagementBeta_when_requestUsesToolUseClearing() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "context_management":{
                    "edits":[{"type":"clear_tool_uses_20250919"}]
                  }
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/anthropic_beta/0").asText())
                .isEqualTo("context-management-2025-06-27");
    }

    @Test
    void test_addsCompactionBeta_when_requestUsesServerCompaction() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "context_management":{
                    "edits":[{"type":"compact_20260112"}]
                  }
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/anthropic_beta/0").asText())
                .isEqualTo("compact-2026-01-12");
    }

    private JsonNode convert(String body, ProtocolConversionRequest requirement) throws Exception {
        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, requirement.streaming()),
                requirement
        );
        return objectMapper.readTree(result.body());
    }
}
