package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OpenAIResponsesRequestConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void test_removesOnlyAuthor_when_longHistoryContainsClientEnvelope(boolean streaming) throws Exception {
        // Arrange
        ObjectNode request = objectMapper.createObjectNode().put("model", "gpt-6-astra").put("stream", streaming);
        ArrayNode input = request.putArray("input");
        for (int index = 0; index < 158; index++) {
            input.addObject().put("role", "user").put("content", "message " + index);
        }
        ObjectNode message = input.addObject().put("type", "message").put("role", "assistant")
                .put("phase", "commentary").put("id", "msg_1").put("status", "completed");
        message.putArray("content").addObject().put("type", "output_text").put("text", "working")
                .putArray("annotations");
        ObjectNode expected = request.deepCopy();
        message.putObject("author").put("role", "assistant");
        ProtocolConversionResult source = passthrough(request.toString(), streaming);

        // Act
        ProtocolConversionResult result = new OpenAIResponsesRequestConverter(new ProtocolJsonSupport(objectMapper)).convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, source.body(), streaming),
                com.api2api.domain.protocol.model.ProtocolConversionRequest.of(streaming, true, true));

        // Assert
        assertThat(objectMapper.readTree(result.body())).isEqualTo(expected);
    }

    @Test
    void test_preservesExactBytes_when_requestUsesLatestFieldsWithoutAuthorExtension() {
        // Arrange
        String body = """
                { "model": "gpt-6-astra", "input": [
                    {"type":"configuration_update","reasoning":{"effort":"max"}},
                    {"type":"reasoning","id":"rs_1","encrypted_content":"opaque","summary":[]},
                    {"type":"function_call","call_id":"call_1","name":"Read","namespace":"files",
                     "arguments":"{}","caller":{"type":"program","caller_id":"prog_1"}}
                  ], "reasoning":{"context":"all_turns","mode":"pro"},
                  "prompt_cache_options":{"mode":"implicit","ttl":"30m"},
                  "access_programs":{"cyber":"standard"}, "future_extension":{"enabled":true} }
                """;
        ProtocolConversionResult source = passthrough(body, false);

        // Act
        ProtocolConversionResult result = convert(source);

        // Assert
        assertThat(result.body()).isEqualTo(source.body());
    }

    @Test
    void test_preservesApplicationAuthorFields_when_normalizingToolHistory() throws Exception {
        // Arrange
        String body = """
                {"model":"gpt-6-astra","metadata":{"author":"application"},
                 "tools":[{"type":"function","name":"write","parameters":{
                   "type":"object","properties":{"author":{"type":"string"}}}}],
                 "input":[{"type":"function_call","author":"client","call_id":"call_1",
                   "name":"write","arguments":"{\\"author\\":\\"Ada\\"}","namespace":"files",
                   "caller":{"type":"program","caller_id":"prog_1"}},
                   {"type":"function_call_output","call_id":"call_1",
                    "output":[{"type":"input_text","text":"{\\"author\\":\\"Ada\\"}"}]}]}
                """;
        ObjectNode expected = (ObjectNode) objectMapper.readTree(body);
        ((ObjectNode) expected.at("/input/0")).remove("author");

        // Act
        ProtocolConversionResult result = convert(passthrough(body, false));

        // Assert
        assertThat(objectMapper.readTree(result.body())).isEqualTo(expected);
    }

    @Test
    void test_preservesTextShorthand_when_inputIsString() {
        // Arrange
        ProtocolConversionResult source = passthrough("{\"model\":\"gpt-6-astra\", \"input\":\"author\"}", false);

        // Act
        ProtocolConversionResult result = convert(source);

        // Assert
        assertThat(result.body()).isEqualTo(source.body());
    }

    @Test
    void test_isIdempotent_when_historyWasAlreadyNormalized() {
        // Arrange
        ProtocolConversionResult source = convert(
                passthrough("{\"input\":[{\"role\":\"user\",\"content\":\"hi\",\"author\":null}]}", false));

        // Act
        ProtocolConversionResult result = convert(source);

        // Assert
        assertThat(result.body()).isEqualTo(source.body());
    }

    private ProtocolConversionResult convert(ProtocolConversionResult source) {
        return new OpenAIResponsesRequestConverter(new ProtocolJsonSupport(objectMapper)).convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, source.body(), false),
                com.api2api.domain.protocol.model.ProtocolConversionRequest.of(false, true, true));
    }

    private ProtocolConversionResult passthrough(String body, boolean streaming) {
        return ProtocolConversionResult.passthrough(ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, streaming));
    }
}
