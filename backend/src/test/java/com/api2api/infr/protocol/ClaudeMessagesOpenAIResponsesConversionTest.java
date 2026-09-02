package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ClaudeMessagesOpenAIResponsesConversionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test_mapsStreamingClaudeToolsAndToolResults_when_convertingToResponsesRequest() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "stream":true,
                  "max_tokens":256,
                  "system":"Be concise",
                  "thinking":{"type":"enabled","budget_tokens":4096},
                  "tools":[{"name":"get_weather","description":"weather","input_schema":{"type":"object"}}],
                  "tool_choice":{"type":"tool","name":"get_weather"},
                  "messages":[
                    {"role":"assistant","content":[{"type":"tool_use","id":"call_1","name":"get_weather","input":{"city":"BJ"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call_1","content":"sunny"}]}
                  ]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, true),
                ProtocolConversionRequest.of(true, true, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("stream").asBoolean()).isTrue();
        assertThat(mapped.at("/tools/0/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/tools/0/parameters/type").asText()).isEqualTo("object");
        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/input/0/role").asText()).isEqualTo("developer");
        assertThat(mapped.at("/input/0/content/0/text").asText()).isEqualTo("Be concise");
        assertThat(mapped.at("/input/1/type").asText()).isEqualTo("function_call");
        assertThat(mapped.at("/input/1/call_id").asText()).isEqualTo("call_1");
        assertThat(mapped.at("/input/2/type").asText()).isEqualTo("function_call_output");
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("medium");
    }

    @Test
    void test_omitsCompletionStatus_when_claudeToolResultReportsError() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "max_tokens":256,
                  "messages":[
                    {"role":"assistant","content":[{
                      "type":"tool_use","id":"call_1","name":"Bash","input":{"command":"git commit"}
                    }]},
                    {"role":"user","content":[{
                      "type":"tool_result","tool_use_id":"call_1","is_error":true,
                      "content":"Exit code 1: nothing to commit"
                    }]}
                  ]
                }
                """;

        // Act
        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        );

        // Assert
        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/input/1").has("status")).isFalse();
    }

    @Test
    void test_acceptsNoopClearThinkingContextManagement_when_claudeCodeUsesResponses() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "stream":true,
                  "max_tokens":8192,
                  "thinking":{"type":"adaptive"},
                  "context_management":{"edits":[{"type":"clear_thinking_20251015","keep":"all"}]},
                  "messages":[{"role":"user","content":"continue"}]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, true),
                ProtocolConversionRequest.of(true, false, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("context_management").isMissingNode()).isTrue();
        assertThat(mapped.path("stream").asBoolean()).isTrue();
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("high");
        assertThat(mapped.at("/input/0/role").asText()).isEqualTo("user");
        assertThat(mapped.at("/input/0/content/0/text").asText()).isEqualTo("continue");
    }

    @Test
    void test_rejectsInvalidThinkingKeepPolicy_when_keepIsNone() {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "max_tokens":8192,
                  "context_management":{"edits":[{"type":"clear_thinking_20251015","keep":"none"}]},
                  "messages":[{"role":"user","content":"continue"}]
                }
                """;

        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        )).hasMessageContaining("CLAUDE_INVALID_CLEAR_THINKING_KEEP");
    }

    @Test
    void test_clearsOldToolResultLocally_when_responsesCannotApplyClaudeContextEditing() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "max_tokens":1024,
                  "context_management":{"edits":[{
                    "type":"clear_tool_uses_20250919",
                    "trigger":{"type":"tool_uses","value":1},
                    "keep":{"type":"tool_uses","value":1}
                  }]},
                  "messages":[
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-1","name":"Read","input":{"path":"old"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-1","content":"old result"}]},
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-2","name":"Read","input":{"path":"current"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-2","content":"current result"}]}
                  ]
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());

        // Assert
        assertThat(mapped.at("/input/1/output").asText())
                .isEqualTo("[Tool result cleared by context management]");
        assertThat(mapped.at("/input/3/output").asText()).isEqualTo("current result");
    }

    @Test
    void test_mapsNonStreamingFunctionCall_when_responsesTargetsClaude() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesResponse(
                        json,
                        new OpenAIResponsesUsageExtractor(),
                        new SseEventTransformer()
                );
        String body = """
                {
                  "id":"resp_1",
                  "model":"gpt-5.5",
                  "status":"completed",
                  "output":[{
                    "type":"function_call",
                    "call_id":"call_1",
                    "name":"get_weather",
                    "arguments":"{\\\"city\\\":\\\"Beijing\\\"}"
                  }],
                  "usage":{"input_tokens":20,"output_tokens":5,"input_tokens_details":{"cached_tokens":3}}
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/content/0/type").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/content/0/id").asText()).isEqualTo("call_1");
        assertThat(mapped.at("/content/0/input/city").asText()).isEqualTo("Beijing");
        assertThat(mapped.path("stop_reason").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/usage/cache_read_input_tokens").asLong()).isEqualTo(3);
    }

    @Test
    void test_doesNotForceReasoningOrVerbosity_when_claudeDidNotRequestThem() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[{"role":"user","content":"hello"}]}
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("reasoning").isMissingNode()).isTrue();
        assertThat(mapped.path("text").isMissingNode()).isTrue();
    }

    @Test
    void test_mapsLatestClaudeOutputToolsFilesAndMcpSchemas_when_convertingToResponses() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "max_tokens":4096,
                  "thinking":{"type":"adaptive"},
                  "output_config":{"effort":"xhigh","format":{"type":"json_schema","schema":{"type":"object"}}},
                  "service_tier":"standard_only",
                  "container":"cntr_123",
                  "tools":[
                    {"name":"local_tool","input_schema":{"type":"object"},"strict":true},
                    {"type":"code_execution_20260120","name":"code_execution"},
                    {"type":"mcp_toolset","mcp_server_name":"docs","default_config":{"enabled":false},"configs":{"search":{"enabled":true}}}
                  ],
                  "mcp_servers":[{"type":"url","name":"docs","url":"https://mcp.example.com","authorization_token":"secret"}],
                  "messages":[{"role":"user","content":[
                    {"type":"image","source":{"type":"url","url":"https://example.com/a.png"}},
                    {"type":"document","title":"doc.pdf","source":{"type":"url","url":"https://example.com/a.pdf"}}
                  ]}]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("xhigh");
        assertThat(mapped.at("/service_tier").asText()).isEqualTo("default");
        assertThat(mapped.at("/text/format/name").asText()).isEqualTo("json_response");
        assertThat(mapped.at("/tools/0/strict").asBoolean()).isTrue();
        assertThat(mapped.at("/tools/1/type").asText()).isEqualTo("code_interpreter");
        assertThat(mapped.at("/tools/1/container").asText()).isEqualTo("cntr_123");
        assertThat(mapped.at("/tools/2/type").asText()).isEqualTo("mcp");
        assertThat(mapped.at("/tools/2/server_url").asText()).isEqualTo("https://mcp.example.com");
        assertThat(mapped.at("/tools/2/authorization").asText()).isEqualTo("secret");
        assertThat(mapped.at("/tools/2/allowed_tools/0").asText()).isEqualTo("search");
        assertThat(mapped.at("/input/0/content/0/image_url").asText()).isEqualTo("https://example.com/a.png");
        assertThat(mapped.at("/input/0/content/1/file_url").asText()).isEqualTo("https://example.com/a.pdf");
    }

    @Test
    void test_mapsFastModeAndRejectsUnknownFields_when_claudeRequestContainsUnsupportedFields() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String fastBody = """
                {"model":"gpt-5.5","max_tokens":256,"speed":"fast","messages":[{"role":"user","content":"hi"}]}
                """;
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, fastBody, false),
                ProtocolConversionRequest.of(false, false, false)).body());
        assertThat(mapped.path("service_tier").asText()).isEqualTo("priority");

        String unknownBody = """
                {"model":"gpt-5.5","max_tokens":256,"future_feature":true,"messages":[]}
                """;
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> converter.convert(
                        ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, unknownBody, false),
                        ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("CLAUDE_RESPONSES_UNSUPPORTED_REQUEST_FIELD: future_feature");
    }

    @Test
    void test_mapsDeprecatedClaudeOutputFormatAndPriorityTier_whenConvertingToResponses() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"service_tier":"priority",
                 "output_format":{"type":"json","schema":{"type":"object"}},
                 "messages":[{"role":"user","content":"hello"}]}
                """;

        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        assertThat(mapped.path("service_tier").asText()).isEqualTo("priority");
        assertThat(mapped.at("/text/format/type").asText()).isEqualTo("json_object");
    }

    @Test
    void test_omitsDefaultAutoServiceTier_whenConvertingToResponses() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"service_tier":"auto",
                 "messages":[{"role":"user","content":"hello"}]}
                """;

        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        assertThat(mapped.path("service_tier").isMissingNode()).isTrue();
    }

    @Test
    void test_normalizesSchemaOnlyOutputFormat_whenConvertingToResponses() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,
                 "output_config":{"format":{"schema":{"type":"object"}}},
                 "messages":[{"role":"user","content":"hello"}]}
                """;

        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        assertThat(mapped.at("/text/format/type").asText()).isEqualTo("json_schema");
        assertThat(mapped.at("/text/format/name").asText()).isEqualTo("json_response");
        assertThat(mapped.at("/text/format/schema/type").asText()).isEqualTo("object");
    }

    @Test
    void test_preservesResponsesToolCallsAndReasoning_whenConvertingToChatResponse() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToOpenAIChatResponse(
                        json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"resp_1","model":"gpt-5.5","status":"completed",
                 "output":[
                   {"type":"reasoning","summary":[{"type":"summary_text","text":"inspect first"}]},
                   {"type":"function_call","call_id":"call_1","name":"Read",
                    "arguments":"{\\"file_path\\":\\"README.md\\"}"},
                   {"type":"message","content":[{"type":"output_text","text":"done"}]}
                 ],"usage":{"input_tokens":3,"output_tokens":2}}
                """;

        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        assertThat(mapped.at("/choices/0/message/content").asText()).isEqualTo("done");
        assertThat(mapped.at("/choices/0/message/reasoning_content").asText()).isEqualTo("inspect first");
        assertThat(mapped.at("/choices/0/message/tool_calls/0/function/name").asText()).isEqualTo("Read");
        assertThat(mapped.at("/choices/0/message/tool_calls/0/function/arguments").asText())
                .isEqualTo("{\"file_path\":\"README.md\"}");
        assertThat(mapped.at("/choices/0/finish_reason").asText()).isEqualTo("tool_calls");
    }

    @Test
    void test_preservesTextAndToolOrdering_when_assistantContentIsInterleaved() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[{"role":"assistant","content":[
                  {"type":"text","text":"before"},
                  {"type":"tool_use","id":"call_1","name":"run","input":{}},
                  {"type":"text","text":"after"}
                ]}]}
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/input/0/content/0/text").asText()).isEqualTo("before");
        assertThat(mapped.at("/input/1/type").asText()).isEqualTo("function_call");
        assertThat(mapped.at("/input/2/content/0/text").asText()).isEqualTo("after");
    }

    @Test
    void test_roundTripsEncryptedResponsesReasoning_when_claudeThinkingSignatureIsUsed() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration(new ProtocolConversionProperties());
        ProtocolMessageConverter responseConverter = configuration.openAIResponsesToClaudeMessagesResponse(
                json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.5","status":"completed","output":[
                  {"type":"reasoning","id":"rs_1","summary":[{"type":"summary_text","text":"summary"}],"encrypted_content":"encrypted"},
                  {"type":"message","role":"assistant","content":[{"type":"output_text","text":"done"}]}
                ],"usage":{"input_tokens":1,"output_tokens":2}}
                """;
        JsonNode claude = objectMapper.readTree(responseConverter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, responseBody, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        String signature = claude.at("/content/0/signature").asText();
        assertThat(signature).startsWith(ResponsesReasoningBridge.SIGNATURE_PREFIX);

        ProtocolMessageConverter requestConverter = configuration.claudeMessagesToOpenAIResponsesRequest(
                json, new SseEventTransformer());
        String requestBody = """
                {"model":"gpt-5.5","max_tokens":256,"thinking":{"type":"adaptive"},"messages":[
                  {"role":"assistant","content":%s}
                ]}
                """.formatted(objectMapper.writeValueAsString(claude.path("content")));
        JsonNode roundTripped = objectMapper.readTree(requestConverter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, requestBody, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        assertThat(roundTripped.at("/input/0/type").asText()).isEqualTo("reasoning");
        assertThat(roundTripped.at("/input/0/id").asText()).isEqualTo("rs_1");
        assertThat(roundTripped.at("/input/0/encrypted_content").asText()).isEqualTo("encrypted");
        assertThat(roundTripped.at("/input/0/summary/0/text").asText()).isEqualTo("summary");
    }

    @Test
    void test_roundTripsProviderHostedToolState_when_opaqueClaudeSignatureIsUsed() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration(new ProtocolConversionProperties());
        ProtocolMessageConverter responseConverter = configuration.openAIResponsesToClaudeMessagesResponse(
                json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.5","status":"completed","output":[
                  {"type":"web_search_call","id":"ws_1","status":"completed","action":{"type":"search","query":"docs"}},
                  {"type":"message","role":"assistant","content":[{"type":"output_text","text":"done"}]}
                ],"usage":{"input_tokens":1,"output_tokens":2}}
                """;
        JsonNode claude = objectMapper.readTree(responseConverter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, responseBody, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        String signature = claude.at("/content/0/signature").asText();
        assertThat(signature).startsWith(ResponsesReasoningBridge.ITEM_SIGNATURE_PREFIX);
        assertThat(claude.at("/content/1/text").asText()).isEqualTo("done");

        ProtocolMessageConverter requestConverter = configuration.claudeMessagesToOpenAIResponsesRequest(
                json, new SseEventTransformer());
        String requestBody = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[
                  {"role":"assistant","content":%s}
                ]}
                """.formatted(objectMapper.writeValueAsString(claude.path("content")));
        JsonNode roundTripped = objectMapper.readTree(requestConverter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, requestBody, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        assertThat(roundTripped.at("/input/0/type").asText()).isEqualTo("web_search_call");
        assertThat(roundTripped.at("/input/0/id").asText()).isEqualTo("ws_1");
        assertThat(roundTripped.at("/input/0/action/query").asText()).isEqualTo("docs");
    }

    @Test
    void test_preservesCommentaryPhase_when_assistantTextPrecedesToolUse() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[{"role":"assistant","content":[
                  {"type":"text","text":"I will inspect the repository first."},
                  {"type":"tool_use","id":"call_1","name":"Read","input":{"path":"README.md"}}
                ]}]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        // Assert
        assertThat(mapped.at("/input/0/phase").asText()).isEqualTo("commentary");
    }

    @Test
    void test_mapsDeferredFunctionAndToolSearch_when_targetIsGpt54OrLater() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"tools":[
                  {"type":"tool_search_tool_regex_20251119","name":"tool_search_tool_regex"},
                  {"name":"Read","description":"Read a file","input_schema":{"type":"object"},
                   "input_examples":[{"path":"README.md"}],"defer_loading":true,
                   "eager_input_streaming":true,"allowed_callers":["direct"]}
                ],"messages":[{"role":"user","content":"inspect"}]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        // Assert
        assertThat(mapped.at("/tools/0/type").asText()).isEqualTo("tool_search");
        assertThat(mapped.at("/tools/1/defer_loading").asBoolean()).isTrue();
        assertThat(mapped.at("/tools/1/description").asText()).contains("README.md");
    }

    @Test
    void test_mapsMaxEffortAndAllTurnsContext_when_encryptedReasoningIsReplayed() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ObjectNode reasoningItem = objectMapper.createObjectNode();
        reasoningItem.put("type", "reasoning");
        reasoningItem.put("id", "rs_1");
        reasoningItem.put("encrypted_content", "encrypted");
        String signature = ResponsesReasoningBridge.encode(objectMapper, reasoningItem).orElseThrow();
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.6","max_tokens":256,"thinking":{"type":"adaptive"},
                 "output_config":{"effort":"max"},"messages":[{"role":"assistant","content":[
                   {"type":"thinking","thinking":"summary","signature":"%s"}
                 ]}]}
                """.formatted(signature);

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("max");
        assertThat(mapped.at("/reasoning/context").asText()).isEqualTo("all_turns");
    }

    @Test
    void test_omitsForeignThinkingState_when_replayingHistoryToResponses() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"summary","signature":"bedrock-opaque-signature"},
                    {"type":"text","text":"visible answer"}
                  ]},
                  {"role":"user","content":"continue"}
                ]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)
        ).body());

        // Assert
        assertThat(mapped.at("/input/0/content/0/text").asText()).isEqualTo("visible answer");
        assertThat(mapped.at("/input/1/content/0/text").asText()).isEqualTo("continue");
    }

    @Test
    void test_capsBudgetDerivedReasoningEffortAtHigh_when_thinkingBudgetExceedsHighThreshold() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.6","max_tokens":32000,"thinking":{"type":"enabled","budget_tokens":31999},
                 "messages":[{"role":"user","content":"hello"}]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("high");
    }

    @Test
    void test_downgradesMaxEffortToXhigh_when_targetPredatesGpt56() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"thinking":{"type":"adaptive"},
                 "output_config":{"effort":"max"},"messages":[{"role":"user","content":"hello"}]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("xhigh");
    }

    @Test
    void test_rejectsCacheOnlyRequest_when_responsesCannotMatchClaudeWarmupSemantics() {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.6","max_tokens":0,
                 "messages":[{"role":"user","content":"warm cache"}]}
                """;

        // Act / Assert
        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)))
                .hasMessageContaining("CLAUDE_RESPONSES_CACHE_ONLY_REQUEST_NOT_SUPPORTED");
    }

    @Test
    void test_preservesOutputItemOrder_when_claudeResponseInterleavesTextAndToolUse() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_order","model":"claude-test","stop_reason":"tool_use",
                 "content":[
                   {"type":"text","text":"before"},
                   {"type":"tool_use","id":"toolu_1","name":"Read","input":{"path":"README.md"}},
                   {"type":"text","text":"after"}
                 ],"usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        // Assert
        assertThat(mapped.at("/output/0/content/0/text").asText()).isEqualTo("before");
        assertThat(mapped.at("/output/1/type").asText()).isEqualTo("function_call");
        assertThat(mapped.at("/output/2/content/0/text").asText()).isEqualTo("after");
        assertThat(mapped.path("output_text").asText()).isEqualTo("beforeafter");
    }

    @Test
    void test_addsIncompleteDetails_when_claudeStopsAtMaxTokens() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_limit","model":"claude-test","stop_reason":"max_tokens",
                 "content":[{"type":"text","text":"partial"}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.path("status").asText()).isEqualTo("incomplete");
        assertThat(mapped.at("/incomplete_details/reason").asText()).isEqualTo("max_output_tokens");
    }

    @Test
    void test_preservesContextWindowStopReason_when_claudeExceedsModelContext() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_context","model":"claude-test",
                 "stop_reason":"model_context_window_exceeded","content":[],
                 "usage":{"input_tokens":100,"output_tokens":0}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.path("status").asText()).isEqualTo("incomplete");
        assertThat(mapped.at("/incomplete_details/reason").asText())
                .isEqualTo("model_context_window_exceeded");
    }

    @Test
    void test_completesResponse_when_claudePausesServerToolLoop() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_pause","model":"claude-test","stop_reason":"pause_turn",
                 "content":[{"type":"text","text":"Continue the server-side loop."}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.path("status").asText()).isEqualTo("completed");
    }

    @Test
    void test_mapsRefusalContent_when_claudeRefusesResponse() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_refusal","model":"claude-test","stop_reason":"refusal",
                 "content":[{"type":"text","text":"I cannot help with that."}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.at("/output/0/content/0/type").asText()).isEqualTo("refusal");
        assertThat(mapped.at("/output/0/content/0/refusal").asText()).isEqualTo("I cannot help with that.");
    }

    @Test
    void test_tunnelsNativeSignatureThroughEncryptedContent_when_claudeThinkingSignatureIsNative() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_reasoning","model":"claude-test","stop_reason":"end_turn",
                 "content":[{"type":"thinking","thinking":"summary","signature":"native-claude-signature"}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/output/0/type").asText()).isEqualTo("reasoning");
        assertThat(mapped.at("/output/0/summary/0/text").asText()).isEqualTo("summary");
        JsonNode restored = ClaudeThinkingStateBridge.decode(
                objectMapper, mapped.at("/output/0/encrypted_content").asText()).orElseThrow();
        assertThat(restored.path("signature").asText()).isEqualTo("native-claude-signature");
    }

    @Test
    void test_restoresEncryptedContent_when_claudeThinkingCarriesResponsesSignature() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ObjectNode originalReasoning = objectMapper.createObjectNode();
        originalReasoning.put("type", "reasoning");
        originalReasoning.put("id", "rs_original");
        originalReasoning.put("encrypted_content", "encrypted-state");
        String signature = ResponsesReasoningBridge.encode(objectMapper, originalReasoning).orElseThrow();
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_reasoning","model":"claude-test","stop_reason":"end_turn",
                 "content":[{"type":"thinking","thinking":"summary","signature":%s}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """.formatted(objectMapper.writeValueAsString(signature));

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/output/0/id").asText()).isEqualTo("rs_original");
        assertThat(mapped.at("/output/0/encrypted_content").asText()).isEqualTo("encrypted-state");
    }

    @Test
    void test_preservesConversationId_when_claudeResponseHasContainer() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_container","model":"claude-test","stop_reason":"end_turn",
                 "container":{"id":"conv_123"},
                 "content":[{"type":"text","text":"done"}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.at("/conversation/id").asText()).isEqualTo("conv_123");
    }

    @Test
    void test_rejectsUnknownContentBlock_when_claudeResponseCannotBeMappedLosslessly() {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_unknown","model":"claude-test","stop_reason":"end_turn",
                 "content":[{"type":"future_server_tool_result","content":[]}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act / Assert
        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("CLAUDE_RESPONSES_UNSUPPORTED_RESPONSE_BLOCK");
    }

    @Test
    void test_bridgesRedactedThinkingThroughEncryptedContent_when_claudeResponseTargetsResponses() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_redacted","model":"claude-test","stop_reason":"end_turn",
                 "content":[{"type":"redacted_thinking","data":"opaque-redacted-data"}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/output/0/type").asText()).isEqualTo("reasoning");
        JsonNode restored = ClaudeThinkingStateBridge.decode(
                objectMapper, mapped.at("/output/0/encrypted_content").asText()).orElseThrow();
        assertThat(restored.path("type").asText()).isEqualTo("redacted_thinking");
        assertThat(restored.path("data").asText()).isEqualTo("opaque-redacted-data");
    }

    @Test
    void test_convertsCompactionBlockToCompactionItem_when_claudeResponseTargetsResponses() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_compaction","model":"claude-test","stop_reason":"end_turn",
                 "content":[{"type":"compaction","content":"conversation summary"},
                            {"type":"text","text":"continuing"}],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.at("/output/0/type").asText()).isEqualTo("compaction");
        assertThat(mapped.at("/output/0/summary/0/text").asText()).isEqualTo("conversation summary");
        assertThat(mapped.at("/output/1/type").asText()).isEqualTo("message");
    }

    @Test
    void test_mapsCacheWriteTokens_when_claudeResponseUsageHasCacheCreation() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesResponse(
                        json, new ClaudeMessagesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"msg_cache","model":"claude-test","stop_reason":"end_turn",
                 "content":[{"type":"text","text":"done"}],
                 "usage":{"input_tokens":10,"output_tokens":2,
                          "cache_creation_input_tokens":7,"cache_read_input_tokens":4}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.at("/usage/input_tokens_details/cache_write_tokens").asLong()).isEqualTo(7);
    }

    @Test
    void test_convertsStructuredInputDirectly_when_responsesRequestTargetsClaude() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"claude-test",
                  "instructions":"Be concise",
                  "max_output_tokens":512,
                  "tools":[{"type":"function","name":"get_weather","description":"weather",
                            "parameters":{"type":"object","properties":{"city":{"type":"string"}}}}],
                  "tool_choice":{"type":"function","name":"get_weather"},
                  "parallel_tool_calls":false,
                  "input":[
                    {"role":"user","content":[{"type":"input_text","text":"weather in BJ?"}]},
                    {"type":"function_call","call_id":"call_1","name":"get_weather","arguments":"{\\"city\\":\\"BJ\\"}"},
                    {"type":"function_call_output","call_id":"call_1","output":"sunny"}
                  ]
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        // Assert
        assertThat(mapped.path("system").asText()).isEqualTo("Be concise");
        assertThat(mapped.path("max_tokens").asInt()).isEqualTo(512);
        assertThat(mapped.at("/tools/0/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.at("/tools/0/input_schema/properties/city/type").asText()).isEqualTo("string");
        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("tool");
        assertThat(mapped.at("/tool_choice/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.at("/tool_choice/disable_parallel_tool_use").asBoolean()).isTrue();
        assertThat(mapped.at("/messages/0/role").asText()).isEqualTo("user");
        assertThat(mapped.at("/messages/1/role").asText()).isEqualTo("assistant");
        assertThat(mapped.at("/messages/1/content/0/type").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/messages/1/content/0/input/city").asText()).isEqualTo("BJ");
        assertThat(mapped.at("/messages/2/role").asText()).isEqualTo("user");
        assertThat(mapped.at("/messages/2/content/0/type").asText()).isEqualTo("tool_result");
        assertThat(mapped.at("/messages/2/content/0/content").asText()).isEqualTo("sunny");
    }

    @Test
    void test_restoresNativeThinkingBlock_when_responsesRequestReplaysBridgedReasoningState() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        String bridged = ClaudeThinkingStateBridge.encode(objectMapper, objectMapper.readTree("""
                {"type":"thinking","thinking":"prior reasoning","signature":"anthropic-signature"}
                """)).orElseThrow();
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"claude-test",
                  "input":[
                    {"role":"user","content":[{"type":"input_text","text":"continue"}]},
                    {"type":"reasoning","id":"rs_1","summary":[],"encrypted_content":%s},
                    {"role":"assistant","content":[{"type":"output_text","text":"ok"}]}
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(bridged));

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/messages/1/role").asText()).isEqualTo("assistant");
        assertThat(mapped.at("/messages/1/content/0/type").asText()).isEqualTo("thinking");
        assertThat(mapped.at("/messages/1/content/0/thinking").asText()).isEqualTo("prior reasoning");
        assertThat(mapped.at("/messages/1/content/0/signature").asText()).isEqualTo("anthropic-signature");
    }

    @Test
    void test_dropsForeignReasoningState_when_responsesRequestCarriesOpenAIEncryptedContent() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"claude-test",
                  "input":[
                    {"role":"user","content":[{"type":"input_text","text":"continue"}]},
                    {"type":"reasoning","id":"rs_1","summary":[],"encrypted_content":"gAAAA-openai-opaque"}
                  ]
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.path("messages")).hasSize(1);
        assertThat(mapped.at("/messages/0/role").asText()).isEqualTo("user");
    }

    @Test
    void test_mapsReasoningEffortToThinkingBudget_when_responsesRequestTargetsClaude() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"claude-test",
                  "max_output_tokens":16384,
                  "reasoning":{"effort":"high"},
                  "input":"think hard"
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/thinking/type").asText()).isEqualTo("enabled");
        assertThat(mapped.at("/thinking/budget_tokens").asInt()).isEqualTo(10240);
        assertThat(mapped.at("/output_config/effort").asText()).isEqualTo("high");
    }

    @Test
    void test_convertsImageDataUriToBase64Source_when_responsesRequestTargetsClaude() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"claude-test",
                  "input":[
                    {"role":"user","content":[
                      {"type":"input_text","text":"describe"},
                      {"type":"input_image","image_url":"data:image/png;base64,aGVsbG8="}
                    ]}
                  ]
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.at("/messages/0/content/1/type").asText()).isEqualTo("image");
        assertThat(mapped.at("/messages/0/content/1/source/type").asText()).isEqualTo("base64");
        assertThat(mapped.at("/messages/0/content/1/source/media_type").asText()).isEqualTo("image/png");
        assertThat(mapped.at("/messages/0/content/1/source/data").asText()).isEqualTo("aGVsbG8=");
    }

    @Test
    void test_mapsTextFormatToOutputConfig_when_responsesRequestUsesJsonSchema() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"claude-test",
                  "text":{"format":{"type":"json_schema","name":"answer",
                          "schema":{"type":"object","properties":{"answer":{"type":"string"}}}}},
                  "input":"answer in json"
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.at("/output_config/format/type").asText()).isEqualTo("json_schema");
        assertThat(mapped.at("/output_config/format/schema/properties/answer/type").asText()).isEqualTo("string");
    }

    @Test
    void test_rejectsStatefulRequest_when_responsesRequestUsesPreviousResponseId() {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"claude-test","previous_response_id":"resp_prev","input":"continue"}
                """;

        // Act / Assert
        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("RESPONSES_CLAUDE_PREVIOUS_RESPONSE_ID_NOT_SUPPORTED");
    }

    @Test
    void test_restoresNativeThinkingBlock_when_responsesResponseCarriesBridgedState() throws Exception {
        // Arrange
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        String bridged = ClaudeThinkingStateBridge.encode(objectMapper, objectMapper.readTree("""
                {"type":"thinking","thinking":"prior reasoning","signature":"anthropic-signature"}
                """)).orElseThrow();
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToClaudeMessagesResponse(
                        json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
        String body = """
                {"id":"resp_1","model":"claude-test","status":"completed",
                 "output":[
                   {"type":"reasoning","id":"rs_1","summary":[],"encrypted_content":%s},
                   {"type":"message","role":"assistant","content":[{"type":"output_text","text":"ok"}]}
                 ],
                 "usage":{"input_tokens":3,"output_tokens":2}}
                """.formatted(objectMapper.writeValueAsString(bridged));

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        // Assert
        assertThat(mapped.at("/content/0/type").asText()).isEqualTo("thinking");
        assertThat(mapped.at("/content/0/thinking").asText()).isEqualTo("prior reasoning");
        assertThat(mapped.at("/content/0/signature").asText()).isEqualTo("anthropic-signature");
    }
}
