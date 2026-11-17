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

class ClaudeResponsesReliabilityTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(mapper);
    private final ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration(new ProtocolConversionProperties());

    @ParameterizedTest
    @ValueSource(strings = {"response.output_text.done", "response.content_part.done", "response.output_item.done", "response.completed"})
    void test_recoversTextSuffixExactlyOnce_when_completionContainsMissingDeltas(String completionType) throws Exception {
        // Arrange
        ObjectNode item = message("Hello world");
        ObjectNode completion = event(completionType);
        switch (completionType) {
            case "response.output_text.done" -> completion.put("text", "Hello world");
            case "response.content_part.done" -> completion.set("part", item.path("content").get(0));
            case "response.output_item.done" -> completion.set("item", item);
            default -> completion.set("response", response(item));
        }
        String prefix = sse(event("response.output_text.delta").put("delta", "Hello")) + sse(completion);

        // Act
        List<JsonNode> events = stream(response(item), prefix);

        // Assert
        assertThat(deltaText(events, "text")).isEqualTo("Hello world");
    }

    @ParameterizedTest
    @ValueSource(strings = {"response.refusal.done", "response.output_item.done"})
    void test_recoversRefusalSuffix_when_onlyPartialRefusalWasStreamed(String completionType) throws Exception {
        // Arrange
        ObjectNode item = message("unused");
        item.putArray("content").addObject().put("type", "refusal").put("refusal", "Cannot comply");
        ObjectNode done = event(completionType);
        if (completionType.equals("response.refusal.done")) done.put("refusal", "Cannot comply");
        else done.set("item", item);

        // Act
        List<JsonNode> events = stream(response(item), sse(event("response.refusal.delta").put("delta", "Cannot")) + sse(done));

        // Assert
        assertThat(deltaText(events, "text")).isEqualTo("Cannot comply");
    }

    @Test
    void test_rejectsConflictingCompletion_when_textCannotBeReconciled() {
        // Arrange
        String prefix = sse(event("response.output_text.delta").put("delta", "First"));

        // Act / Assert
        assertThatThrownBy(() -> stream(response(message("Different")), prefix))
                .isInstanceOf(java.io.IOException.class).hasMessageContaining("differs from emitted deltas");
    }

    @Test
    void test_recoversToolArgumentSuffix_when_doneContainsMoreThanDeltas() throws Exception {
        // Arrange
        ObjectNode tool = tool("{\"path\":\"file.txt\"}");
        String prefix = sse(event("response.output_item.added").set("item", tool.deepCopy().put("arguments", "")))
                + sse(event("response.function_call_arguments.delta").put("delta", "{\"path\":"))
                + sse(event("response.function_call_arguments.done").put("arguments", tool.path("arguments").asText()));

        // Act
        List<JsonNode> events = stream(response(tool), prefix);

        // Assert
        assertThat(deltaText(events, "partial_json")).isEqualTo(tool.path("arguments").asText());
    }

    @Test
    void test_usesRealCallIdentity_when_addedEventIsMissing() throws Exception {
        // Arrange
        ObjectNode tool = tool("{\"path\":\"file.txt\"}");
        String prefix = sse(event("response.function_call_arguments.delta").put("item_id", "fc_item").put("delta", "{\"path\":"))
                + sse(event("response.function_call_arguments.done").put("item_id", "fc_item").put("arguments", tool.path("arguments").asText()));
        JsonNode expected = mapper.readTree("""
                {"type":"tool_use","id":"call_real","name":"lookup","input":{}}
                """);

        // Act
        List<JsonNode> events = stream(response(tool), prefix);

        // Assert
        assertThat(events.stream().filter(e -> "tool_use".equals(e.path("content_block").path("type").asText())))
                .singleElement().satisfies(e -> assertThat(e.path("content_block")).isEqualTo(expected));
    }

    @ParameterizedTest
    @CsvSource({"max_output_tokens,max_tokens", "content_filter,refusal"})
    void test_keepsTerminalReason_when_responseAlsoContainsTool(String reason, String expected) throws Exception {
        // Arrange
        ObjectNode response = response(tool("{}"));
        response.put("status", "incomplete").putObject("incomplete_details").put("reason", reason);

        // Act
        JsonNode result = convertResponse(response);

        // Assert
        assertThat(result.path("stop_reason").asText()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"max_output_tokens,max_tokens", "content_filter,refusal"})
    void test_keepsTerminalReason_when_streamAlsoContainsTool(String reason, String expected) throws Exception {
        // Arrange
        ObjectNode response = response(tool("{}"));
        response.put("status", "incomplete").putObject("incomplete_details").put("reason", reason);

        // Act
        List<JsonNode> events = stream(response, "");

        // Assert
        assertThat(deltaText(events, "stop_reason")).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"commentary", "final_answer"})
    void test_reportsRefusal_when_completedResponseContainsRefusal(String phase) throws Exception {
        // Arrange
        ObjectNode item = message("").put("phase", phase);
        item.putArray("content").addObject().put("type", "refusal").put("refusal", "Cannot comply");

        // Act
        JsonNode result = convertResponse(response(item));

        // Assert
        assertThat(result.path("stop_reason").asText()).isEqualTo("refusal");
    }

    @Test
    void test_preservesUnicodeCitation_when_annotationUsesCodePointOffsets() throws Exception {
        // Arrange
        ObjectNode item = citedMessage();

        // Act
        JsonNode result = convertResponse(response(item));

        // Assert
        assertThat(result.at("/content/0/citations/0/cited_text").asText()).isEqualTo("资料");
    }

    @Test
    void test_restoresCitationAnnotation_when_messagesHistoryIsReplayed() throws Exception {
        // Arrange
        ObjectNode item = citedMessage();
        ObjectNode request = request();
        request.putArray("messages").addObject().put("role", "assistant")
                .set("content", convertResponse(response(item)).path("content"));

        // Act
        JsonNode result = convertRequest(request);

        // Assert
        assertThat(result.at("/input/0/content/0/annotations")).isEqualTo(item.at("/content/0/annotations"));
    }

    @Test
    void test_emitsCitationOnceBeforeStop_when_annotationAppearsInMultipleEvents() throws Exception {
        // Arrange
        ObjectNode item = citedMessage();
        String prefix = sse(event("response.output_text.delta").put("delta", "😀 资料"))
                + sse(event("response.output_text.annotation.added").put("annotation_index", 0)
                        .set("annotation", item.at("/content/0/annotations/0")))
                + sse(event("response.content_part.done").set("part", item.at("/content/0")))
                + sse(event("response.output_item.done").set("item", item));

        // Act
        List<JsonNode> events = stream(response(item), prefix);

        // Assert
        List<String> lifecycle = events.stream().map(e -> e.path("delta").path("type").asText(e.path("type").asText()))
                .filter(type -> List.of("citations_delta", "content_block_stop").contains(type)).toList();
        assertThat(lifecycle).containsExactly("citations_delta", "content_block_stop");
    }

    @Test
    void test_preservesCitationsPerContentBlock_when_oneMessageHasMultipleParts() throws Exception {
        // Arrange
        ObjectNode item = message("plain");
        item.withArray("content").add(citedMessage().at("/content/0"));

        // Act
        List<JsonNode> events = stream(response(item), "");

        // Assert
        assertThat(events.stream().filter(e -> "citations_delta".equals(e.path("delta").path("type").asText())))
                .singleElement().satisfies(e -> assertThat(e.path("index").asInt()).isEqualTo(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 99})
    void test_keepsUrlWithoutInvalidExcerpt_when_citationOffsetsAreOutOfBounds(int end) throws Exception {
        // Arrange
        ObjectNode item = citedMessage();
        ((ObjectNode) item.at("/content/0/annotations/0")).put("end_index", end);

        // Act
        JsonNode result = convertResponse(response(item));

        // Assert
        assertThat(result.at("/content/0/citations/0/cited_text").asText()).isEmpty();
    }

    @Test
    void test_enablesStrictStructuredOutput_when_schemaHasRequiredNestedProperties() throws Exception {
        // Arrange
        ObjectNode request = request();
        request.putObject("output_config").set("format", mapper.readTree("""
                {"type":"json_schema","schema":{"type":"object","properties":{"result":{
                 "type":"array","items":{"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}}},"required":["result"]}}
                """));

        // Act
        JsonNode format = convertRequest(request).at("/text/format");

        // Assert
        assertThat(format).isEqualTo(mapper.readTree("""
                {"type":"json_schema","name":"json_response","strict":true,"schema":{"type":"object","additionalProperties":false,
                 "properties":{"result":{"type":"array","items":{"type":"object","additionalProperties":false,
                 "properties":{"answer":{"type":"string"}},"required":["answer"]}}},"required":["result"]}}
                """));
    }

    @Test
    void test_preservesOptionalToolInputs_when_strictIsNotRequested() throws Exception {
        // Arrange
        ObjectNode request = request();
        JsonNode schema = mapper.readTree("{\"type\":\"object\",\"properties\":{\"limit\":{\"type\":\"integer\"}}}");
        request.putArray("tools").addObject().put("name", "lookup").set("input_schema", schema);

        // Act
        JsonNode tool = convertRequest(request).at("/tools/0");

        // Assert
        assertThat(tool).isEqualTo(mapper.createObjectNode().put("name", "lookup").put("type", "function")
                .put("strict", false).set("parameters", schema));
    }

    @Test
    void test_rejectsIncompatibleExplicitStrict_when_optionalPropertiesWouldChangeMeaning() throws Exception {
        // Arrange
        ObjectNode request = request();
        request.putObject("output_config").set("format", mapper.readTree("""
                {"type":"json_schema","strict":true,"schema":{"type":"object","properties":{"answer":{"type":"string"}}}}
                """));

        // Act / Assert
        assertThatThrownBy(() -> convertRequest(request)).hasMessageContaining("STRICT_SCHEMA_REQUIRES");
    }

    @Test
    void test_closesNullableObjects_when_strictToolHasNestedDefinitions() throws Exception {
        // Arrange
        ObjectNode request = request();
        request.putArray("tools").addObject().put("name", "lookup").put("strict", true).set("input_schema", mapper.readTree("""
                {"type":"object","properties":{"filter":{"$ref":"#/$defs/filter"}},"required":["filter"],
                 "$defs":{"filter":{"type":["object","null"],"properties":{}}}}
                """));

        // Act
        JsonNode result = convertRequest(request);

        // Assert
        assertThat(result.at("/tools/0/parameters/$defs/filter")).isEqualTo(mapper.readTree("""
                {"type":["object","null"],"properties":{},"required":[],"additionalProperties":false}
                """));
    }

    @Test
    void test_keepsExplicitNonStrictSchema_when_formatContainsOptionalProperties() throws Exception {
        // Arrange
        ObjectNode request = request();
        JsonNode format = mapper.readTree("""
                {"type":"json_schema","name":"result","strict":false,"schema":{"type":"object",
                 "properties":{"answer":{"type":"string"}},"additionalProperties":true}}
                """);
        request.putObject("output_config").set("format", format);

        // Act
        JsonNode result = convertRequest(request);

        // Assert
        assertThat(result.at("/text/format")).isEqualTo(format);
    }

    @Test
    void test_preservesSchemaExampleData_when_strictSchemaContainsSchemaLikeValues() throws Exception {
        // Arrange
        ObjectNode request = request();
        JsonNode example = mapper.readTree("{\"type\":\"object\",\"properties\":{\"data\":true}}");
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("example").put("type", "string").set("default", example);
        schema.putArray("required").add("example");
        request.putArray("tools").addObject().put("name", "lookup").put("strict", true).set("input_schema", schema);

        // Act
        JsonNode result = convertRequest(request);

        // Assert
        assertThat(result.at("/tools/0/parameters/properties/example/default")).isEqualTo(example);
    }

    @ParameterizedTest
    @CsvSource({"thread,SERVER_THREAD_NOT_SUPPORTED", "fallbacks,PROVIDER_FALLBACK_NOT_SUPPORTED", "fallback_credit_token,PROVIDER_FALLBACK_NOT_SUPPORTED"})
    void test_rejectsProviderStateExplicitly_when_newMessagesFieldsCannotBeConverted(String field, String reason) {
        // Arrange
        ObjectNode request = request();
        request.put(field, "provider-owned-state");

        // Act / Assert
        assertThatThrownBy(() -> convertRequest(request)).hasMessageContaining(reason);
    }

    @Test
    void test_flushesPendingCitation_when_terminalSnapshotOmitsAnnotations() throws Exception {
        // Arrange
        ObjectNode item = citedMessage();
        String prefix = sse(event("response.output_text.delta").put("delta", "😀 资料"))
                + sse(event("response.output_text.annotation.added").put("annotation_index", 0)
                        .set("annotation", item.at("/content/0/annotations/0")));
        ((ObjectNode) item.at("/content/0")).remove("annotations");

        // Act
        List<JsonNode> events = stream(response(item), prefix);

        // Assert
        assertThat(events.stream().filter(e -> "citations_delta".equals(e.path("delta").path("type").asText())))
                .singleElement().satisfies(e -> assertThat(e.at("/delta/citation/cited_text").asText()).isEqualTo("资料"));
    }

    @Test
    void test_rejectsCorruptedCitationState_when_historyEnvelopeIsInvalid() {
        // Arrange
        ObjectNode request = request();
        request.putArray("messages").addObject().put("role", "assistant").putArray("content")
                .addObject().put("type", "text").put("text", "source").putArray("citations")
                .addObject().put("encrypted_index", "api2api_url_v1_!");

        // Act / Assert
        assertThatThrownBy(() -> convertRequest(request)).hasMessageContaining("INVALID_CITATION_STATE");
    }

    @Test
    void test_rejectsConflictingToolCompletion_when_argumentsDifferFromSentPrefix() {
        // Arrange
        ObjectNode tool = tool("{\"path\":\"other.txt\"}");
        String prefix = sse(event("response.output_item.added").set("item", tool.deepCopy().put("arguments", "")))
                + sse(event("response.function_call_arguments.delta").put("delta", "{\"path\":\"original"));

        // Act / Assert
        assertThatThrownBy(() -> stream(response(tool), prefix)).hasMessageContaining("differs from emitted deltas");
    }

    @Test
    void test_recoversEachToolIndependently_when_parallelCallsInterleaveDeltas() throws Exception {
        // Arrange
        ObjectNode first = tool("{\"path\":\"first\"}");
        ObjectNode second = tool("{\"path\":\"second\"}").put("call_id", "call_second").put("id", "fc_second");
        ObjectNode response = response(first);
        response.withArray("output").add(second);
        String prefix = sse(event("response.output_item.added").set("item", first.deepCopy().put("arguments", "")))
                + sse(event("response.output_item.added").put("output_index", 1).set("item", second.deepCopy().put("arguments", "")))
                + sse(event("response.function_call_arguments.delta").put("output_index", 1).put("delta", "{\"path\":"))
                + sse(event("response.function_call_arguments.delta").put("delta", "{"));

        // Act
        List<JsonNode> events = stream(response, prefix);

        // Assert
        assertThat(List.of(deltaText(events.stream().filter(e -> e.path("index").asInt(-1) == 0).toList(), "partial_json"),
                deltaText(events.stream().filter(e -> e.path("index").asInt(-1) == 1).toList(), "partial_json")))
                .containsExactly(first.path("arguments").asText(), second.path("arguments").asText());
    }

    private ObjectNode request() {
        ObjectNode request = mapper.createObjectNode().put("model", "gpt-6-astra").put("max_tokens", 1024);
        request.putArray("messages").addObject().put("role", "user").put("content", "Hello");
        return request;
    }

    private ObjectNode message(String text) {
        ObjectNode item = mapper.createObjectNode().put("type", "message").put("role", "assistant");
        item.putArray("content").addObject().put("type", "output_text").put("text", text);
        return item;
    }

    private ObjectNode citedMessage() {
        ObjectNode item = message("😀 资料");
        ((ObjectNode) item.at("/content/0")).putArray("annotations").addObject().put("type", "url_citation")
                .put("url", "https://example.com/source").put("title", "Source").put("start_index", 2).put("end_index", 4);
        return item;
    }

    private ObjectNode tool(String arguments) {
        return mapper.createObjectNode().put("type", "function_call").put("id", "fc_item")
                .put("call_id", "call_real").put("name", "lookup").put("arguments", arguments);
    }

    private ObjectNode response(JsonNode item) {
        ObjectNode response = mapper.createObjectNode().put("id", "resp_1").put("model", "gpt-6-astra").put("status", "completed");
        response.putArray("output").add(item);
        return response;
    }

    private ObjectNode event(String type) {
        return mapper.createObjectNode().put("type", type).put("output_index", 0).put("content_index", 0);
    }

    private List<JsonNode> stream(ObjectNode response, String prefix) throws Exception {
        String wire = prefix + sse(mapper.createObjectNode().put("type", "response." + response.path("status").asText()).set("response", response));
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();
        new UnifiedStreamingConversionAdapter(mapper).transform(
                GatewayStreamingConversionContext.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("claude-client"), ProviderChannelId.of(1L), ModelName.of("gpt-6-astra")),
                new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)), downstream);
        List<JsonNode> events = new ArrayList<>();
        for (String line : downstream.toString(StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith("data: ")) events.add(mapper.readTree(line.substring(6)));
        }
        return events;
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
        return mapper.readTree(configuration.openAIResponsesToClaudeMessagesResponse(json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer())
                .convert(ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, source.toString(), false),
                        ProtocolConversionRequest.of(false, true, false)).body());
    }

    private String sse(JsonNode event) {
        return "event: " + event.path("type").asText() + "\ndata: " + event + "\n\n";
    }
}
