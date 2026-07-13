package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClaudeMessagesOpenAIResponsesLatestFeaturesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
    private final ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration();

    @Test
    void test_mapsProgrammaticToolCalling_when_claudeToolAllowsCodeExecutionCaller() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"gpt-5.6",
                  "max_tokens":1024,
                  "tools":[{
                    "name":"Read",
                    "input_schema":{"type":"object"},
                    "allowed_callers":["direct","code_execution_20260521"]
                  }],
                  "messages":[{"role":"user","content":"inspect"}]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, true);

        // Assert
        assertThat(mapped.path("tools").toString()).isEqualTo("""
                [{"type":"programmatic_tool_calling"},{"type":"function","name":"Read","allowed_callers":["direct","programmatic"],"parameters":{"type":"object","properties":{}},"strict":false}]""");
    }

    @Test
    void test_preservesProgramCaller_when_toolResultIsReplayed() throws Exception {
        // Arrange
        String responseBody = """
                {
                  "id":"resp_1",
                  "model":"gpt-5.6",
                  "status":"completed",
                  "output":[
                    {"type":"program","id":"prog_1","call_id":"call_prog_1","code":"await tools.Read({});","fingerprint":"opaque"},
                    {
                      "type":"function_call",
                      "call_id":"call_1",
                      "name":"Read",
                      "arguments":%s,
                      "caller":{"type":"program","caller_id":"call_prog_1"}
                    }
                  ],
                  "usage":{"input_tokens":1,"output_tokens":1}
                }
                """.formatted(objectMapper.writeValueAsString("{\"file_path\":\"README.md\"}"));
        JsonNode claude = convertResponse(responseBody);
        assertThat(claude.at("/content/1/id").asText())
                .isEqualTo(claude.at("/content/2/caller/tool_id").asText());
        assertThat(claude.at("/content/2/caller/type").asText())
                .isEqualTo("code_execution_20260521");
        String requestBody = """
                {
                  "model":"gpt-5.6",
                  "max_tokens":1024,
                  "messages":[
                    {"role":"assistant","content":%s},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call_1","content":"ok"}]}
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(claude.path("content")));

        // Act
        JsonNode replayed = convertRequest(requestBody, true);

        // Assert
        assertThat(replayed.at("/input/0/type").asText()).isEqualTo("program");
        assertThat(replayed.at("/input/1/caller").toString())
                .isEqualTo("{\"type\":\"program\",\"caller_id\":\"call_prog_1\"}");
        assertThat(replayed.at("/input/2/caller").toString())
                .isEqualTo("{\"type\":\"program\",\"caller_id\":\"call_prog_1\"}");
        assertThat(replayed.at("/input/2/output").asText()).isEqualTo("ok");
    }

    @Test
    void test_omitsExperimentalCacheBreakpoints_when_targetIsGpt56() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"gpt-5.6",
                  "max_tokens":1024,
                  "system":[{"type":"text","text":"stable system","cache_control":{"type":"ephemeral","ttl":"1h"}}],
                  "messages":[{"role":"user","content":[
                    {"type":"text","text":"stable prompt","cache_control":{"type":"ephemeral","ttl":"5m"}}
                  ]}]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, false);

        // Assert
        assertThat(mapped.at("/input/0/content/0/prompt_cache_breakpoint").isMissingNode()).isTrue();
        assertThat(mapped.at("/input/1/content/0/prompt_cache_breakpoint").isMissingNode()).isTrue();
        assertThat(mapped.path("prompt_cache_options").isMissingNode()).isTrue();
    }

    @Test
    void test_rejectsNonTextResult_when_programmaticCallIsPending() {
        // Arrange
        String programToolId = ResponsesProgrammaticToolBridge.toClaudeProgramToolId("call_prog_1");
        String body = """
                {"model":"gpt-5.6","max_tokens":1024,"messages":[
                  {"role":"assistant","content":[{
                    "type":"tool_use","id":"call_1","name":"Read","input":{},
                    "caller":{"type":"code_execution_20260120","tool_id":"%s"}
                  }]},
                  {"role":"user","content":[{
                    "type":"tool_result","tool_use_id":"call_1","content":[{
                      "type":"image","source":{"type":"base64","media_type":"image/png","data":"AA=="}
                    }]
                  }]}
                ]}
                """.formatted(programToolId);

        // Act / Assert
        assertThatThrownBy(() -> convertRequest(body, true))
                .hasMessageContaining("CLAUDE_RESPONSES_PROGRAMMATIC_TOOL_RESULT_MUST_BE_TEXT");
    }

    @Test
    void test_mapsProgramOutputToCodeExecutionResult_when_programCompletes() throws Exception {
        // Arrange
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.6","status":"completed","output":[
                  {"type":"program_output","id":"prog_out_1","call_id":"call_prog_1",
                   "result":%s,"status":"completed"}
                ],"usage":{"input_tokens":1,"output_tokens":1}}
                """.formatted(objectMapper.writeValueAsString("{\"answer\":42}"));

        // Act
        JsonNode claude = convertResponse(responseBody);

        // Assert
        assertThat(claude.at("/content/1/type").asText())
                .isEqualTo("code_execution_tool_result");
        assertThat(claude.at("/content/1/content/stdout").asText())
                .isEqualTo("{\"answer\":42}");
        assertThat(claude.at("/content/1/content/return_code").asInt()).isZero();
    }

    @Test
    void test_keepsPromptCacheKeyStable_when_followupTurnIsAppended() throws Exception {
        // Arrange
        String first = """
                {"model":"gpt-5.6","max_tokens":1024,"system":"stable",
                 "messages":[{"role":"user","content":"first"}]}
                """;
        String followup = """
                {"model":"gpt-5.6","max_tokens":1024,"system":"stable",
                 "messages":[
                   {"role":"user","content":"first"},
                   {"role":"assistant","content":"answer"},
                   {"role":"user","content":"follow up"}
                 ]}
                """;

        // Act
        String firstKey = convertRequest(first, false).path("prompt_cache_key").asText();
        String followupKey = convertRequest(followup, false).path("prompt_cache_key").asText();

        // Assert
        assertThat(followupKey).isEqualTo(firstKey);
    }

    @Test
    void test_mapsPauseTurn_when_responseOnlyContainsReplayableState() throws Exception {
        // Arrange
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.6","status":"completed","output":[
                  {"type":"compaction","id":"cmp_1","encrypted_content":"encrypted-summary"}
                ],"usage":{"input_tokens":1,"output_tokens":1}}
                """;

        // Act
        JsonNode claude = convertResponse(responseBody);

        // Assert
        assertThat(claude.path("stop_reason").asText()).isEqualTo("pause_turn");
        assertThat(claude.path("content")).anySatisfy(block -> {
            assertThat(block.path("type").asText()).isEqualTo("text");
            assertThat(block.path("text").asText()).isEqualTo("Conversation compacted.");
        });
    }

    @Test
    void test_prunesPreCompactionHistory_when_openAiCompactionStateIsReplayed() throws Exception {
        // Arrange
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.6","status":"completed","output":[
                  {"type":"compaction","id":"cmp_1","encrypted_content":"encrypted-summary"}
                ],"usage":{"input_tokens":1,"output_tokens":1}}
                """;
        JsonNode claude = convertResponse(responseBody);
        String requestBody = """
                {"model":"gpt-5.6","max_tokens":1024,"messages":[
                  {"role":"user","content":"obsolete history"},
                  {"role":"assistant","content":%s},
                  {"role":"user","content":"continue"}
                ]}
                """.formatted(objectMapper.writeValueAsString(claude.path("content")));

        // Act
        JsonNode replayed = convertRequest(requestBody, false);

        // Assert
        assertThat(replayed.path("input").size()).isEqualTo(2);
        assertThat(replayed.at("/input/0/type").asText()).isEqualTo("compaction");
        assertThat(replayed.at("/input/0/id").asText()).isEqualTo("cmp_1");
        assertThat(replayed.at("/input/1/content/0/text").asText()).isEqualTo("continue");
    }

    @Test
    void test_usesReadableSummary_when_nativeClaudeCompactionIsReceived() throws Exception {
        // Arrange
        String body = """
                {"model":"gpt-5.6","max_tokens":1024,"messages":[
                  {"role":"user","content":"obsolete history"},
                  {"role":"assistant","content":[{"type":"compaction","content":"decisions and pending work"}]},
                  {"role":"user","content":"continue"}
                ]}
                """;

        // Act
        JsonNode mapped = convertRequest(body, false);

        // Assert
        assertThat(mapped.path("input").size()).isEqualTo(2);
        assertThat(mapped.at("/input/0/type").asText()).isEqualTo("message");
        assertThat(mapped.at("/input/0/content/0/text").asText())
                .isEqualTo("decisions and pending work");
    }

    @Test
    void test_roundTripsCustomToolCall_when_claudeReturnsToolResult() throws Exception {
        // Arrange
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.6","status":"completed","output":[
                  {"type":"custom_tool_call","call_id":"custom_1","name":"apply_patch","input":"*** Begin Patch"}
                ],"usage":{"input_tokens":1,"output_tokens":1}}
                """;
        JsonNode claude = convertResponse(responseBody);
        String toolUseId = claude.at("/content/0/id").asText();
        String requestBody = """
                {"model":"gpt-5.6","max_tokens":1024,"messages":[
                  {"role":"assistant","content":%s},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"%s","content":"done"}]}
                ]}
                """.formatted(objectMapper.writeValueAsString(claude.path("content")), toolUseId);

        // Act
        JsonNode replayed = convertRequest(requestBody, true);

        // Assert
        assertThat(replayed.at("/input/0/type").asText()).isEqualTo("custom_tool_call");
        assertThat(replayed.at("/input/0/call_id").asText()).isEqualTo("custom_1");
        assertThat(replayed.at("/input/0/input").asText()).isEqualTo("*** Begin Patch");
        assertThat(replayed.at("/input/1/type").asText()).isEqualTo("custom_tool_call_output");
        assertThat(replayed.at("/input/1/call_id").asText()).isEqualTo("custom_1");
    }

    @Test
    void test_removesEmptyPages_when_responsesCallsClaudeCodeReadTool() throws Exception {
        // Arrange
        String body = """
                {"id":"resp_1","model":"gpt-5.6","status":"completed","output":[
                  {"type":"function_call","call_id":"call_1","name":"Read",
                   "arguments":%s}
                ],"usage":{"input_tokens":1,"output_tokens":1}}
                """.formatted(objectMapper.writeValueAsString(
                        "{\"file_path\":\"README.md\",\"pages\":\"\"}"));

        // Act
        JsonNode mapped = convertResponse(body);

        // Assert
        assertThat(mapped.at("/content/0/input").toString())
                .isEqualTo("{\"file_path\":\"README.md\"}");
    }

    @Test
    void test_mapsCacheWriteUsage_when_openAiReportsExplicitCachePopulation() throws Exception {
        // Arrange
        String body = """
                {"id":"resp_1","model":"gpt-5.6","status":"completed","output":[],
                 "usage":{"input_tokens":20,"output_tokens":2,
                          "input_tokens_details":{"cached_tokens":3,"cache_write_tokens":4}}}
                """;

        // Act
        JsonNode mapped = convertResponse(body);

        // Assert
        assertThat(mapped.path("usage").toString())
                .isEqualTo("{\"input_tokens\":13,\"output_tokens\":2,\"cache_creation_input_tokens\":4,\"cache_read_input_tokens\":3}");
    }

    @Test
    void test_usesOutputTextFallback_when_outputItemsAreMissing() throws Exception {
        // Arrange
        String body = """
                {"id":"resp_1","model":"gpt-5.6","status":"completed","output":[],
                 "output_text":"fallback answer","usage":{"input_tokens":1,"output_tokens":1}}
                """;

        // Act
        JsonNode mapped = convertResponse(body);

        // Assert
        assertThat(mapped.at("/content/0/text").asText()).isEqualTo("fallback answer");
    }

    @Test
    void test_throwsConversionError_when_openAiResponseFailed() {
        // Arrange
        String body = """
                {"id":"resp_1","model":"gpt-5.6","status":"failed","output":[],
                 "error":{"message":"sandbox unavailable"}}
                """;

        // Act / Assert
        assertThatThrownBy(() -> convertResponse(body))
                .hasMessageContaining("RESPONSES_CLAUDE_RESPONSE_FAILED: sandbox unavailable");
    }

    private JsonNode convertRequest(String body, boolean toolCalling) throws Exception {
        ProtocolMessageConverter converter = configuration.claudeMessagesToOpenAIResponsesRequest(
                json, new SseEventTransformer());
        return objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, toolCalling, true)
        ).body());
    }

    private JsonNode convertResponse(String body) throws Exception {
        ProtocolMessageConverter converter = configuration.openAIResponsesToClaudeMessagesResponse(
                json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
        return objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, true, true)
        ).body());
    }
}
