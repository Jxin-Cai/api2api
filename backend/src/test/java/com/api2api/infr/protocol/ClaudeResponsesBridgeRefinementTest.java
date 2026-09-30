package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClaudeResponsesBridgeRefinementTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(mapper);
    private final ProtocolConverterConfiguration configuration =
            new ProtocolConverterConfiguration(new ProtocolConversionProperties());

    @Test
    void test_preservesOriginalReasoningItem_when_thinkingIsReplayed() throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"reasoning","id":"rs_1","encrypted_content":"opaque","status":"completed",
                 "summary":[{"type":"summary_text","text":"First"},{"type":"summary_text","text":"Second"}]}
                """);
        JsonNode block = convertResponse(response(item)).path("content").get(0);
        ObjectNode request = mapper.createObjectNode().put("model", "gpt-6-astra").put("max_tokens", 1024);
        request.putArray("messages").addObject().put("role", "assistant").putArray("content").add(block);
        // Act
        JsonNode converted = convertRequest(request);
        // Assert
        assertThat(converted.path("input").get(0)).isEqualTo(item);
    }

    @Test
    void test_emitsSummary_when_onlyReasoningItemDoneArrives() throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"reasoning","id":"rs_1","encrypted_content":"opaque",
                 "summary":[{"type":"summary_text","text":"First"},{"type":"summary_text","text":"Second"}]}
                """);
        // Act
        List<JsonNode> events = stream(item, "");
        // Assert
        assertThat(deltaText(events, "thinking")).isEqualTo("FirstSecond");
    }

    @ParameterizedTest
    @CsvSource({"commentary,pause_turn", "final_answer,end_turn"})
    void test_preservesTurnBoundary_when_responseHasPhase(String phase, String stop) throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("{\"type\":\"message\",\"phase\":\"" + phase
                + "\",\"content\":[{\"type\":\"output_text\",\"text\":\"hello\"}]}");
        // Act
        JsonNode converted = convertResponse(response(item));
        // Assert
        assertThat(converted.path("stop_reason").asText()).isEqualTo(stop);
    }

    @ParameterizedTest
    @CsvSource({"commentary,pause_turn", "final_answer,end_turn"})
    void test_preservesTurnBoundary_when_phaseArrivesAfterText(String phase, String stop) throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("{\"type\":\"message\",\"phase\":\"" + phase
                + "\",\"content\":[{\"type\":\"output_text\",\"text\":\"hello\"}]}");
        String prefix = sse(mapper.readTree("""
                {"type":"response.output_text.delta","output_index":0,"delta":"hello"}
                """));
        // Act
        List<JsonNode> events = stream(item, prefix);
        // Assert
        assertThat(deltaText(events, "stop_reason")).isEqualTo(stop);
    }

    @Test
    void test_omitsMcpServer_when_legacyConfigurationDisablesIt() throws Exception {
        // Arrange
        ObjectNode request = mcpRequest("{\"enabled\":false}", "{}");
        // Act
        JsonNode result = convertRequest(request);
        // Assert
        assertThat(result.path("tools")).noneMatch(tool -> "mcp".equals(tool.path("type").asText()));
    }

    @Test
    void test_preservesMcpAllowlist_when_legacyConfigurationRestrictsTools() throws Exception {
        // Arrange
        ObjectNode request = mcpRequest("{\"allowed_tools\":[\"read\"]}", "{}");
        // Act
        JsonNode result = convertRequest(request);
        // Assert
        assertThat(result.at("/tools/0/allowed_tools")).isEqualTo(mapper.readTree("[\"read\"]"));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "false,false"})
    void test_loadsMcpToolsAccordingToDefaults_when_configUsesNestedDeferLoading(boolean deferred, boolean expected) throws Exception {
        // Arrange
        ObjectNode request = mcpRequest("{}", "{\"default_config\":{\"defer_loading\":" + deferred + "}}");
        // Act
        JsonNode result = convertRequest(request);
        // Assert
        assertThat(result.path("tools")).anySatisfy(tool -> {
            assertThat(tool.path("type").asText()).isEqualTo("mcp");
            assertThat(tool.path("defer_loading").asBoolean()).isEqualTo(expected);
        });
    }

    @Test
    void test_eagerlyLoadsMcpServer_when_enabledToolsHaveMixedLoadingModes() throws Exception {
        // Arrange
        ObjectNode request = mcpRequest("{}", """
                {"default_config":{"defer_loading":true},"configs":{"read":{"defer_loading":false}}}
                """);
        // Act
        JsonNode result = convertRequest(request);
        // Assert
        assertThat(result.at("/tools/0/defer_loading").asBoolean()).isFalse();
    }

    private ObjectNode mcpRequest(String legacy, String toolset) throws Exception {
        ObjectNode request = mapper.createObjectNode().put("model", "gpt-6-astra").put("max_tokens", 1024);
        request.putArray("messages").addObject().put("role", "user").put("content", "Use the tools");
        request.putArray("mcp_servers").addObject().put("type", "url").put("name", "server")
                .put("url", "https://example.com/mcp").set("tool_configuration", mapper.readTree(legacy));
        ObjectNode tool = (ObjectNode) mapper.readTree(toolset);
        tool.put("type", "mcp_toolset").put("mcp_server_name", "server");
        request.putArray("tools").add(tool);
        return request;
    }

    @Test
    void test_preservesConversation_when_foreignRedactedThinkingAppearsInHistory() throws Exception {
        // Arrange
        JsonNode request = mapper.readTree("""
                {"model":"gpt-6-astra","max_tokens":1024,"messages":[
                  {"role":"assistant","content":[{"type":"redacted_thinking","data":"opaque-claude-state"},
                    {"type":"text","text":"Earlier answer"}]},
                  {"role":"user","content":"Continue"}]}
                """);
        // Act
        JsonNode result = convertRequest(request);
        // Assert: only the unusable provider-specific state is omitted.
        assertThat(result.path("input")).isEqualTo(mapper.readTree("""
                [{"type":"message","role":"assistant","phase":"final_answer",
                  "content":[{"type":"output_text","text":"Earlier answer"}]},
                 {"type":"message","role":"user","content":[{"type":"input_text","text":"Continue"}]}]
                """));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void test_preservesAllTextParts_when_messageItemCompletes(boolean firstPartStreamed) throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"message","phase":"commentary","content":[
                  {"type":"output_text","text":"First"},{"type":"output_text","text":"Second"}]}
                """);
        String prefix = firstPartStreamed ? sse(mapper.readTree("""
                {"type":"response.output_text.delta","output_index":0,"content_index":0,"delta":"First"}
                """)) : "";
        // Act
        List<JsonNode> events = stream(item, prefix);
        // Assert: the second part must not be mistaken for a duplicate of the first.
        assertThat(deltaText(events, "text")).isEqualTo("FirstSecond");
    }

    @Test
    void test_deduplicatesEachTextPart_when_partsAndItemBothComplete() throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"message","content":[
                  {"type":"output_text","text":"First"},{"type":"output_text","text":"Second"}]}
                """);
        String prefix = sse(mapper.readTree("""
                {"type":"response.output_text.done","output_index":0,"content_index":0,"text":"First"}
                """)) + sse(mapper.readTree("""
                {"type":"response.output_text.done","output_index":0,"content_index":1,"text":"Second"}
                """));
        // Act
        List<JsonNode> events = stream(item, prefix);
        // Assert
        assertThat(deltaText(events, "text")).isEqualTo("FirstSecond");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void test_preservesSummaryParts_when_summaryUsesMultipleIndexes(boolean firstPartStreamed) throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"reasoning","id":"rs_1","encrypted_content":"opaque","summary":[
                  {"type":"summary_text","text":"First"},{"type":"summary_text","text":"Second"}]}
                """);
        String prefix = firstPartStreamed ? sse(mapper.readTree("""
                {"type":"response.reasoning_summary_text.delta","output_index":0,"summary_index":0,"delta":"Fir"}
                """)) : "";
        prefix += sse(mapper.readTree("""
                {"type":"response.reasoning_summary_text.done","output_index":0,"summary_index":0,"text":"First"}
                """)) + sse(mapper.readTree("""
                {"type":"response.reasoning_summary_part.done","output_index":0,"summary_index":1,
                 "part":{"type":"summary_text","text":"Second"}}
                """));
        // Act
        List<JsonNode> events = stream(item, prefix);
        // Assert
        assertThat(deltaText(events, "thinking")).isEqualTo("FirstSecond");
    }

    @Test
    void test_keepsCommentaryPhase_when_completionOmitsPreviouslyDeclaredPhase() throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"message","content":[{"type":"output_text","text":"Working"}]}
                """);
        String prefix = sse(mapper.readTree("""
                {"type":"response.output_item.added","output_index":0,
                 "item":{"type":"message","phase":"commentary","content":[]}}
                """));
        // Act
        List<JsonNode> events = stream(item, prefix);
        // Assert
        assertThat(deltaText(events, "stop_reason")).isEqualTo("pause_turn");
    }

    @Test
    void test_preservesRefusalStopReason_when_commentaryContainsRefusal() throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"message","phase":"commentary","content":[{"type":"refusal","refusal":"Cannot comply"}]}
                """);
        // Act
        List<JsonNode> events = stream(item, "");
        // Assert
        assertThat(deltaText(events, "stop_reason")).isEqualTo("refusal");
    }

    private ObjectNode response(JsonNode item) {
        ObjectNode result = mapper.createObjectNode().put("id", "resp_1").put("model", "gpt-6-astra").put("status", "completed");
        result.putArray("output").add(item);
        return result;
    }

    private List<JsonNode> stream(JsonNode item, String prefix) throws Exception {
        ObjectNode done = mapper.createObjectNode().put("type", "response.output_item.done").put("output_index", 0);
        done.set("item", item);
        ObjectNode completed = mapper.createObjectNode().put("type", "response.completed");
        completed.set("response", response(item));
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();
        new UnifiedStreamingConversionAdapter(mapper).transform(
                GatewayStreamingConversionContext.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("claude-client"), ProviderChannelId.of(1L), ModelName.of("gpt-6-astra")),
                new ByteArrayInputStream((prefix + sse(done) + sse(completed)).getBytes(StandardCharsets.UTF_8)), downstream);
        return dataEvents(downstream.toString(StandardCharsets.UTF_8));
    }

    private String deltaText(List<JsonNode> events, String field) {
        return events.stream().map(event -> event.path("delta").path(field).asText("")).reduce("", String::concat);
    }

    private JsonNode convertRequest(JsonNode source) throws Exception {
        return mapper.readTree(configuration.claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer())
                .convert(ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, source.toString(), false),
                        ProtocolConversionRequest.of(false, true, false)).body());
    }

    private JsonNode convertResponse(JsonNode source) throws Exception {
        return mapper.readTree(configuration.openAIResponsesToClaudeMessagesResponse(json, new OpenAIResponsesUsageExtractor(),
                        new SseEventTransformer())
                .convert(ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, source.toString(), false),
                        ProtocolConversionRequest.of(false, true, false)).body());
    }

    private String sse(JsonNode event) {
        return "event: " + event.path("type").asText() + "\ndata: " + event + "\n\n";
    }

    private List<JsonNode> dataEvents(String wire) throws Exception {
        List<JsonNode> events = new ArrayList<>();
        for (String line : wire.split("\n")) {
            if (line.startsWith("data: ")) {
                events.add(mapper.readTree(line.substring(6)));
            }
        }
        return events;
    }
}
