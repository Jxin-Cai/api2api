package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClaudeResponsesCurrentSchemaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void test_usesEasyInputMessage_when_claudeAssistantHistoryHasNoProviderOutputId() throws Exception {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.withArray("messages").addObject().put("role", "assistant")
                .putArray("content").addObject().put("type", "text").put("text", "previous answer");

        // Act
        JsonNode result = convert(request, false);

        // Assert
        assertThat(result.at("/input/1")).isEqualTo(mapper.readTree("""
                {"type":"message","role":"assistant","phase":"final_answer",
                 "content":[{"type":"input_text","text":"previous answer"}]}
                """));
    }

    @Test
    void test_rejectsTooSmallBudget_when_claudeLimitIsBelowResponsesMinimum() {
        // Arrange
        ObjectNode request = request("gpt-6-astra").put("max_tokens", 8);

        // Act / Assert
        assertThatThrownBy(() -> convert(request, false))
                .hasMessageContaining("CLAUDE_RESPONSES_MAX_OUTPUT_TOKENS_MUST_BE_AT_LEAST_16");
    }

    @Test
    void test_omitsReasoning_when_nonReasoningModelReceivesDisabledThinking() throws Exception {
        // Arrange
        ObjectNode request = request("gpt-4.1");
        request.putObject("thinking").put("type", "disabled");

        // Act
        JsonNode result = convert(request, false);

        // Assert
        assertThat(result.has("reasoning")).isFalse();
    }

    @Test
    void test_omitsSummary_when_reasoningIsDisabled() throws Exception {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.putObject("thinking").put("type", "disabled");

        // Act
        JsonNode result = convert(request, false);

        // Assert
        assertThat(result.path("reasoning")).isEqualTo(mapper.readTree("{\"effort\":\"none\"}"));
    }

    @Test
    void test_preservesLegacySchema_when_outputConfigOnlyContainsEffort() throws Exception {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.putObject("output_config").put("effort", "high");
        request.set("output_format", mapper.readTree("""
                {"type":"json_schema","schema":{"type":"object","properties":{"answer":{"type":"string"}}}}
                """));

        // Act
        JsonNode result = convert(request, false);

        // Assert
        assertThat(result.at("/text/format")).isEqualTo(mapper.readTree("""
                {"type":"json_schema","name":"json_response",
                 "schema":{"type":"object","properties":{"answer":{"type":"string"}}}}
                """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "json_object", "text"})
    void test_emitsOnlyType_when_outputFormatDoesNotAcceptSchema(String type) throws Exception {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.putObject("output_config").putObject("format").put("type", type)
                .put("name", "unused").putObject("schema").put("type", "object");

        // Act
        JsonNode result = convert(request, false);

        // Assert
        assertThat(result.at("/text/format")).isEqualTo(
                mapper.createObjectNode().put("type", "json".equals(type) ? "json_object" : type));
    }

    @ParameterizedTest
    @CsvSource({"gpt-5.5,false", "gpt-5.6,true", "gpt-6-astra,true"})
    void test_gatesExplicitCacheParameters_when_upstreamCacheSupportIsEnabled(String model, boolean supported) throws Exception {
        // Arrange
        ObjectNode request = request(model);
        request.putObject("cache_control").put("type", "ephemeral").put("ttl", "1h");

        // Act
        JsonNode result = convert(request, true);

        // Assert
        ObjectNode actual = mapper.createObjectNode();
        actual.set("options", result.path("prompt_cache_options"));
        actual.set("breakpoint", result.at("/input/0/content/0/prompt_cache_breakpoint"));
        ObjectNode expected = mapper.createObjectNode();
        if (supported) {
            expected.putObject("options").put("mode", "explicit").put("ttl", "30m");
            expected.putObject("breakpoint").put("mode", "explicit");
        } else {
            expected.set("options", com.fasterxml.jackson.databind.node.MissingNode.getInstance());
            expected.set("breakpoint", com.fasterxml.jackson.databind.node.MissingNode.getInstance());
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_omitsExplicitCacheParameters_when_configurationUsesDefault() throws Exception {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.putObject("cache_control").put("type", "ephemeral");

        // Act
        JsonNode result = convert(request, false);

        // Assert
        assertThat(result.toString()).doesNotContain("prompt_cache_options", "prompt_cache_breakpoint");
    }

    @ParameterizedTest
    @CsvSource({
            "thinking,between_tools,CLAUDE_RESPONSES_THINKING_TYPE_NOT_SUPPORTED",
            "thinking,future_mode,CLAUDE_RESPONSES_THINKING_TYPE_NOT_SUPPORTED",
            "tool_choice,future_mode,CLAUDE_RESPONSES_INVALID_TOOL_CHOICE",
            "compaction,summarize,CLAUDE_RESPONSES_EXPLICIT_COMPACTION_NOT_SUPPORTED"
    })
    void test_rejectsUnrepresentableMode_when_conversionWouldLoseSemantics(
            String field, String type, String error) {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.putObject(field).put("type", type);

        // Act / Assert
        assertThatThrownBy(() -> convert(request, false))
                .isInstanceOf(ProtocolConversionException.class).hasMessageContaining(error);
    }

    @Test
    void test_rejectsUnknownEffort_when_invalidEffortIsProvided() {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.putObject("output_config").put("effort", "typo");

        // Act / Assert
        assertThatThrownBy(() -> convert(request, false))
                .hasMessageContaining("CLAUDE_RESPONSES_INVALID_REASONING_EFFORT");
    }

    @Test
    void test_rejectsTransientSystemMessage_when_lifetimeCannotBePreserved() {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.withArray("messages").addObject().put("role", "system")
                .put("content", "temporary").put("clear_at", "next_user_message");

        // Act / Assert
        assertThatThrownBy(() -> convert(request, false))
                .hasMessageContaining("CLAUDE_RESPONSES_MESSAGE_CLEAR_AT_NOT_SUPPORTED");
    }

    @Test
    void test_rejectsPerMessageEffort_when_turnConfigurationCannotBePreserved() {
        // Arrange
        ObjectNode request = request("gpt-6-astra");
        request.withArray("messages").addObject().put("role", "system")
                .put("content", "work").putObject("output_config").put("effort", "max");

        // Act / Assert
        assertThatThrownBy(() -> convert(request, false))
                .hasMessageContaining("CLAUDE_RESPONSES_MESSAGE_OUTPUT_CONFIG_NOT_SUPPORTED");
    }

    private ObjectNode request(String model) {
        ObjectNode request = mapper.createObjectNode().put("model", model).put("max_tokens", 256);
        request.putArray("messages").addObject().put("role", "user").put("content", "hello");
        return request;
    }

    private JsonNode convert(ObjectNode request, boolean explicitCache) throws Exception {
        ProtocolConversionProperties properties = new ProtocolConversionProperties();
        properties.setResponsesExplicitCacheBreakpointsEnabled(explicitCache);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(properties)
                .claudeMessagesToOpenAIResponsesRequest(new ProtocolJsonSupport(mapper), new SseEventTransformer());
        return mapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, request.toString(), false),
                ProtocolConversionRequest.of(false, false, true)).body());
    }
}
