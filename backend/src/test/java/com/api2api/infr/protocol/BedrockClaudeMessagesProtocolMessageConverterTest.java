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
    void test_preservesNestedProviderPrefixedFields_when_responseContainsToolInput() throws Exception {
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
                  "content":[{
                    "type":"tool_use",
                    "id":"toolu_1",
                    "name":"inspect_payload",
                    "input":{"amazon-bedrock-customer-field":"must-survive"}
                  }],
                  "amazon-bedrock-guardrailAction":"NONE",
                  "usage":{"input_tokens":1,"output_tokens":1}
                }
                """;

        // Act
        ProtocolConversionResult result = responseConverter.convert(
                ProtocolPayload.of(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        );
        JsonNode mapped = objectMapper.readTree(result.body());

        // Assert
        assertThat(mapped.at("/content/0/input/amazon-bedrock-customer-field").asText())
                .isEqualTo("must-survive");
    }

    @Test
    void test_preservesConversationVerbatim_when_historyContainsRepeatedSuccessfulToolCalls() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[
                    {"role":"assistant","content":[{
                      "type":"tool_use","id":"toolu_1","name":"Read","input":{"file_path":"README.md"}
                    }]},
                    {"role":"user","content":[{
                      "type":"tool_result","tool_use_id":"toolu_1","content":"first"
                    }]},
                    {"role":"assistant","content":[{
                      "type":"tool_use","id":"toolu_2","name":"Read","input":{"file_path":"README.md"}
                    }]},
                    {"role":"user","content":[{
                      "type":"tool_result","tool_use_id":"toolu_2","content":"second"
                    }]}
                  ]
                }
                """;
        JsonNode sourceMessages = objectMapper.readTree(body).path("messages");

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.path("messages")).isEqualTo(sourceMessages);
    }

    @Test
    void test_ignoresToolInputObjects_when_theyResembleMediaContentBlocks() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{
                    "role":"assistant",
                    "content":[{
                      "type":"tool_use",
                      "id":"toolu_1",
                      "name":"store_record",
                      "input":{"type":"image","source":{"type":"business-record"}}
                    }]
                  }]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/messages/0/content/0/input/source/type").asText())
                .isEqualTo("business-record");
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
    void test_addsEffortBeta_when_outputConfigUsesEffort() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"analyze this"}],
                  "output_config":{"effort":"high"}
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/anthropic_beta/0").asText())
                .isEqualTo("effort-2025-11-24");
    }

    @Test
    void test_removesThinkingDisplay_when_claudeCodeCallsBedrockInvokeModel() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-sonnet-4-6",
                  "max_tokens":32000,
                  "messages":[{"role":"user","content":"Return structured output"}],
                  "tools":[{
                    "name":"StructuredOutput",
                    "description":"Return the final response in the requested structured format.",
                    "input_schema":{
                      "type":"object",
                      "properties":{"answer":{"type":"string"}},
                      "required":["answer"],
                      "additionalProperties":false
                    }
                  }],
                  "thinking":{"type":"adaptive","display":"omitted"},
                  "output_config":{"effort":"high"},
                  "stream":true
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(true, true, true));

        // Assert
        assertThat(mapped.at("/thinking/type").asText()).isEqualTo("adaptive");
        assertThat(mapped.path("thinking").has("display")).isFalse();
    }

    @Test
    void test_wrapsStructuredOutputSchema_when_workflowRequiresRootArray() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"Select dimensions"}],
                  "tools":[{
                    "name":"StructuredOutput",
                    "input_schema":{"type":"array","items":{"type":"string"}}
                  }]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, true, false));

        // Assert
        assertThat(mapped.at("/tools/0/input_schema/type").asText()).isEqualTo("object");
        assertThat(mapped.at("/tools/0/input_schema/properties/__api2api_structured_output/type").asText())
                .isEqualTo("array");
    }

    @Test
    void test_unwrapsStructuredOutputInput_when_bedrockReturnsWrappedArray() throws Exception {
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
                  "content":[{
                    "type":"tool_use",
                    "id":"toolu_1",
                    "name":"StructuredOutput",
                    "input":{"__api2api_structured_output":["design","security"]}
                  }],
                  "usage":{"input_tokens":1,"output_tokens":1}
                }
                """;

        // Act
        ProtocolConversionResult result = responseConverter.convert(
                ProtocolPayload.of(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        );
        JsonNode mapped = objectMapper.readTree(result.body());

        // Assert
        assertThat(mapped.at("/content/0/input"))
                .containsExactly(
                        objectMapper.getNodeFactory().textNode("design"),
                        objectMapper.getNodeFactory().textNode("security")
                );
    }

    @Test
    void test_wrapsStructuredOutputHistory_when_followUpContainsRootArrayInput() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[
                    {"role":"assistant","content":[{
                      "type":"tool_use",
                      "id":"toolu_1",
                      "name":"StructuredOutput",
                      "input":["design","security"]
                    }]},
                    {"role":"user","content":[{
                      "type":"tool_result",
                      "tool_use_id":"toolu_1",
                      "content":"accepted"
                    }]}
                  ],
                  "tools":[{
                    "name":"StructuredOutput",
                    "input_schema":{"type":"array","items":{"type":"string"}}
                  }]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, true, false));

        // Assert
        assertThat(mapped.at("/messages/0/content/0/input/__api2api_structured_output"))
                .containsExactly(
                        objectMapper.getNodeFactory().textNode("design"),
                        objectMapper.getNodeFactory().textNode("security")
                );
    }

    @Test
    void test_forcesStructuredOutput_when_claudeCodeSendsEnforcementRetry() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[
                    {"role":"user","content":"Review the code"},
                    {"role":"assistant","content":"The review is complete."},
                    {"role":"user","content":[{
                      "type":"text",
                      "text":"[structured-output-enforce] You MUST call the StructuredOutput tool to complete this request. Call this tool now."
                    }]}
                  ],
                  "tools":[{
                    "name":"StructuredOutput",
                    "input_schema":{
                      "type":"object",
                      "properties":{"result":{"type":"string"}},
                      "required":["result"]
                    }
                  }],
                  "thinking":{"type":"adaptive","display":"omitted"}
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, true, false));

        // Assert
        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("tool");
        assertThat(mapped.at("/tool_choice/name").asText()).isEqualTo("StructuredOutput");
        assertThat(mapped.at("/tool_choice/disable_parallel_tool_use").asBoolean()).isTrue();
        assertThat(mapped.has("thinking")).isFalse();
    }

    @Test
    void test_preservesAutomaticToolChoice_when_structuredOutputIsNotEnforcementRetry() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"Review the code"}],
                  "tools":[{
                    "name":"StructuredOutput",
                    "input_schema":{"type":"object","properties":{"result":{"type":"string"}}}
                  }],
                  "thinking":{"type":"adaptive","display":"omitted"}
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, true, false));

        // Assert
        assertThat(mapped.has("tool_choice")).isFalse();
        assertThat(mapped.at("/thinking/type").asText()).isEqualTo("adaptive");
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
    void test_mapsWebSearchToCustomTool_when_requestUsesAnthropicServerTool() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"search the web"}],
                  "tools":[{
                    "type":"web_search_20250305",
                    "name":"web_search",
                    "max_uses":5,
                    "allowed_callers":["direct"],
                    "allowed_domains":["docs.aws.amazon.com","docs.anthropic.com"],
                    "user_location":{
                      "type":"approximate",
                      "city":"Shanghai",
                      "country":"CN",
                      "timezone":"Asia/Shanghai"
                    }
                  }]
                }
                """;

        // Act
        JsonNode mapped = convert(body, ProtocolConversionRequest.of(false, false, false));

        // Assert
        assertThat(mapped.at("/tools/0/type").asText()).isEqualTo("custom");
        assertThat(mapped.at("/tools/0/name").asText()).isEqualTo("web_search");
        assertThat(mapped.at("/tools/0/input_schema/type").asText()).isEqualTo("object");
        assertThat(mapped.at("/tools/0/input_schema/properties/query/type").asText()).isEqualTo("string");
        assertThat(mapped.at("/tools/0/input_schema/required/0").asText()).isEqualTo("query");
        assertThat(mapped.at("/tools/0/input_schema/additionalProperties").asBoolean()).isFalse();
        assertThat(mapped.at("/tools/0/allowed_callers").isMissingNode()).isTrue();
        assertThat(mapped.at("/tools/0/description").asText())
                .contains("at most 5 times")
                .contains("docs.aws.amazon.com, docs.anthropic.com")
                .contains("Shanghai")
                .contains("Asia/Shanghai");
    }

    @Test
    void test_rejectsWebSearchDomainFilters_when_allowedAndBlockedDomainsAreCombined() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"search the web"}],
                  "tools":[{
                    "type":"web_search_20250305",
                    "name":"web_search",
                    "allowed_domains":["example.com"],
                    "blocked_domains":["blocked.example.com"]
                  }]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("allowed_domains and blocked_domains");
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

    @Test
    void test_rejectsDuplicateToolName_when_toolsReuseName() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "tools":[
                    {"name":"lookup","input_schema":{"type":"object"}},
                    {"name":"lookup","input_schema":{"type":"object"}}
                  ]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("tool names must be unique")
                .hasMessageContaining("lookup");
    }

    @Test
    void test_rejectsNonBooleanToolOption_when_deferLoadingIsString() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "tools":[{
                    "name":"lookup",
                    "input_schema":{"type":"object"},
                    "defer_loading":"true"
                  }]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("defer_loading")
                .hasMessageContaining("boolean");
    }

    @Test
    void test_rejectsInputExamples_when_valueIsNotArray() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "tools":[{
                    "name":"lookup",
                    "input_schema":{"type":"object"},
                    "input_examples":{"query":"weather"}
                  }]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("input_examples must be an array");
    }

    @Test
    void test_rejectsCustomTool_when_inputSchemaIsNotObjectSchema() {
        // Arrange
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":128,
                  "messages":[{"role":"user","content":"hello"}],
                  "tools":[{
                    "name":"lookup",
                    "input_schema":{"type":"string"}
                  }]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> convert(body, ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("input_schema.type must be 'object'");
    }

    private JsonNode convert(String body, ProtocolConversionRequest requirement) throws Exception {
        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, requirement.streaming()),
                requirement
        );
        return objectMapper.readTree(result.body());
    }
}
