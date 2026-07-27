package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void test_mapsOnlyBedrockSupportedBetaFeatures_when_claudeCodeCallsBedrockInvokeModel() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}]
                }
                """;
        ProtocolConversionRequest requirement = ConversionRequirement.of(true, false, false)
                .withAnthropicBetaFeatures(List.of(
                        "claude-code-20250219",
                        "interleaved-thinking-2025-05-14",
                        "thinking-token-count-2026-05-13",
                        "context-management-2025-06-27",
                        "prompt-caching-scope-2026-01-05"
                ))
                .forRoute(1783929967772706L, "claude-opus-4.6")
                .toProtocolConversionRequest();

        // Act
        JsonNode mapped = convert(body, requirement);

        // Assert
        assertThat(mapped.path("anthropic_beta"))
                .containsExactly(
                        objectMapper.getNodeFactory().textNode("interleaved-thinking-2025-05-14"),
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

    @Test
    void test_preservesFallbackCreditBeta_when_requestUsesFallbackCredit() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "fallback_credit_token":"credit-token"
                }
                """;
        ProtocolConversionRequest requirement = ConversionRequirement.of(false, false, false)
                .withAnthropicBetaFeatures(List.of("fallback-credit-2026-06-09"))
                .toProtocolConversionRequest();

        // Act
        JsonNode mapped = convert(body, requirement);

        // Assert
        assertThat(mapped.at("/anthropic_beta/0").asText())
                .isEqualTo("fallback-credit-2026-06-09");
    }

    @Test
    void test_rejectsUnsupportedField_when_requestUsesClaudePlatformMcp() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "mcp_servers":[{"name":"docs","url":"https://example.com/mcp"}]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("mcp_servers")
                .hasMessageContaining("no Bedrock InvokeModel equivalent");
    }

    @Test
    void test_removesBedrockExtensions_when_responseContainsProviderMetadata() throws Exception {
        // Arrange
        BedrockClaudeMessagesProtocolMessageConverter responseConverter =
                new BedrockClaudeMessagesProtocolMessageConverter(
                        new ProtocolJsonSupport(objectMapper),
                        new ClaudeMessagesUsageExtractor(),
                        ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                        ProtocolType.CLAUDE_MESSAGES,
                        ProtocolConversionDirection.RESPONSE,
                        new SseEventTransformer()
                );
        String body = """
                {
                  "type":"message",
                  "content":[],
                  "amazon-bedrock-guardrailAction":"NONE",
                  "usage":{"input_tokens":1,"output_tokens":1}
                }
                """;

        // Act
        ProtocolConversionResult result = responseConverter.convert(
                ProtocolPayload.of(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        );

        // Assert
        assertThat(objectMapper.readTree(result.body()).has("amazon-bedrock-guardrailAction"))
                .isFalse();
    }

    @Test
    void test_preservesBase64Image_when_requestUsesBedrockSupportedSource() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{
                    "role":"user",
                    "content":[{
                      "type":"image",
                      "source":{"type":"base64","media_type":"image/png","data":"aGVsbG8="}
                    }]
                  }]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/messages/0/content/0/source/type").asText()).isEqualTo("base64");
    }

    @Test
    void test_rejectsImageSource_when_requestUsesUrl() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{
                    "role":"user",
                    "content":[{
                      "type":"image",
                      "source":{"type":"url","url":"https://example.com/image.png"}
                    }]
                  }]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("image source only supports base64")
                .hasMessageContaining("url");
    }

    @Test
    void test_rejectsDocumentSource_when_requestUsesFileId() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{
                    "role":"user",
                    "content":[{
                      "type":"document",
                      "source":{"type":"file","file_id":"file_123"}
                    }]
                  }]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("document source only supports base64")
                .hasMessageContaining("file");
    }

    @Test
    void test_addsComputerUseBeta_when_requestUsesLatestComputerTool() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.7",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"take a screenshot"}],
                  "tools":[{
                    "type":"computer_20251124",
                    "name":"computer",
                    "display_width_px":1024,
                    "display_height_px":768
                  }]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/anthropic_beta/0").asText())
                .isEqualTo("computer-use-2025-11-24");
    }

    @Test
    void test_addsFineGrainedStreamingBeta_when_toolEnablesEagerInputStreaming() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"write a file"}],
                  "tools":[{
                    "name":"write_file",
                    "input_schema":{"type":"object"},
                    "eager_input_streaming":true
                  }]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/anthropic_beta/0").asText())
                .isEqualTo("fine-grained-tool-streaming-2025-05-14");
    }

    @Test
    void test_addsToolSearchBeta_when_requestUsesDeferredTools() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"find a tool"}],
                  "tools":[
                    {"type":"tool_search_tool_regex","name":"tool_search_tool_regex"},
                    {"name":"weather","input_schema":{"type":"object"},"defer_loading":true}
                  ]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/anthropic_beta/0").asText())
                .isEqualTo("tool-search-tool-2025-10-19");
    }

    @Test
    void test_rejectsToolType_when_requestUsesUnsupportedServerTool() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"search the web"}],
                  "tools":[{"type":"web_search_20250305","name":"web_search"}]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("web_search_20250305")
                .hasMessageContaining("not supported by Bedrock InvokeModel");
    }

    @Test
    void test_rejectsDeferredTools_when_noToolIsImmediatelyLoaded() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"find a tool"}],
                  "tools":[
                    {"name":"weather","input_schema":{"type":"object"},"defer_loading":true}
                  ]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("at least one tool with defer_loading=false");
    }

    private JsonNode convert(String body, ProtocolConversionRequest requirement) throws Exception {
        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, requirement.streaming()),
                requirement
        );
        return objectMapper.readTree(result.body());
    }
}
