package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ConversionImplementationStatus;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingDirection;
import com.api2api.domain.protocol.model.MappingDocument;
import com.api2api.domain.protocol.model.MappingLossiness;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultProtocolConversionServiceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-19T00:00:00Z");

    private final DefaultProtocolConversionServiceAdapter adapter =
            new DefaultProtocolConversionServiceAdapter(
                    List.of(new OpenAIResponsesRequestConverter(new ProtocolJsonSupport(new ObjectMapper()))),
                    List.of(), new ObjectMapper());

    @Test
    void test_forwardsBodyUnchanged_when_clientAndUpstreamProtocolsMatch() {
        // Arrange
        String body = """
                {"model":"claude-opus-4-8",\
                "context_management":{"edits":[{"type":"compact_20260112"}]},\
                "messages":[{"role":"assistant","content":[\
                {"type":"thinking","thinking":"summary","signature":"foreign-provider-signature"}]}]}""";
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false);

        // Act
        ProtocolConversionResult result = adapter.convertRequest(
                payload,
                ProtocolType.CLAUDE_MESSAGES,
                requirement(),
                List.of(definition(ProtocolType.CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES))
        );

        // Assert
        assertThat(result.body()).isEqualTo(body);
    }

    @Test
    void test_marksResultAsPassthrough_when_clientAndUpstreamProtocolsMatch() {
        // Arrange
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, "{\"messages\":[]}", false);

        // Act
        ProtocolConversionResult result = adapter.convertRequest(
                payload,
                ProtocolType.CLAUDE_MESSAGES,
                requirement(),
                List.of(definition(ProtocolType.CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES))
        );

        // Assert
        assertThat(result.passthrough()).isTrue();
    }

    private ProtocolConversionRequest requirement() {
        return ProtocolConversionRequest.of(false, false, false);
    }

    @Test
    void test_removesAuthor_when_responsesRequestPassesThrough() throws Exception {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES,
                "{\"model\":\"gpt-6-astra\",\"input\":[{\"role\":\"user\",\"content\":\"hi\",\"author\":\"client\"}]}", false);

        // Act
        ProtocolConversionResult result = adapter.convertRequest(payload, ProtocolType.OPENAI_RESPONSES,
                requirement(), List.of(definition(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)));

        // Assert
        assertThat(mapper.readTree(result.body()).at("/input/0")).isEqualTo(
                mapper.readTree("{\"role\":\"user\",\"content\":\"hi\"}"));
    }

    @Test
    void test_normalizesAssistantText_when_responsesRequestUsesSameProtocolRoute() throws Exception {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        ProtocolPayload payload = ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, """
                {"model":"gpt-6-astra","input":[{"role":"assistant","phase":"final_answer",
                  "content":[{"type":"input_text","text":"Earlier answer"}]}]}
                """, false);

        // Act
        ProtocolConversionResult result = adapter.convertRequest(payload, ProtocolType.OPENAI_RESPONSES,
                requirement(), List.of(definition(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)));

        // Assert
        assertThat(mapper.readTree(result.body()).at("/input/0")).isEqualTo(mapper.readTree("""
                {"role":"assistant","phase":"final_answer","content":[{"type":"output_text","text":"Earlier answer"}]}
                """));
    }

    @Test
    void test_normalizesReplayedReasoning_when_messagesConvertToResponses() throws Exception {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        var item = mapper.readTree("""
                {"type":"reasoning","id":"rs_1","encrypted_content":"opaque","summary":[],
                 "status":"completed","author":"upstream-extension"}
                """);
        String signature = ResponsesReasoningBridge.encode(mapper, item).orElseThrow();
        var request = mapper.createObjectNode().put("model", "gpt-6-astra").put("max_tokens", 128);
        request.putArray("messages").addObject().put("role", "assistant").putArray("content")
                .addObject().put("type", "thinking").put("thinking", "summary").put("signature", signature);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .claudeMessagesToOpenAIResponsesRequest(new ProtocolJsonSupport(mapper), new SseEventTransformer());
        DefaultProtocolConversionServiceAdapter bridge = new DefaultProtocolConversionServiceAdapter(
                List.of(converter), List.of(), mapper);

        // Act
        ProtocolConversionResult result = bridge.convertRequest(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, request.toString(), false), ProtocolType.OPENAI_RESPONSES,
                ProtocolConversionRequest.of(false, false, true),
                List.of(definition(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES)));

        // Assert
        assertThat(mapper.readTree(result.body()).at("/input/0")).isEqualTo(mapper.readTree("""
                {"type":"reasoning","id":"rs_1","encrypted_content":"opaque","summary":[],"status":"completed"}
                """));
    }

    private ProtocolConversionDefinition definition(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        return ProtocolConversionDefinition.create(
                ProtocolConversionDefinitionId.of(1L),
                sourceProtocol,
                targetProtocol,
                ConversionCapability.of(true, true, true, true, true,
                        Set.of(ContentMappingType.TEXT, ContentMappingType.TOOL_CALL)),
                mapping(MappingDirection.REQUEST),
                mapping(MappingDirection.RESPONSE),
                ConversionImplementationStatus.IMPLEMENTED,
                NOW
        );
    }

    private MappingDocument mapping(MappingDirection direction) {
        return MappingDocument.of(
                direction,
                direction.name() + " passthrough",
                "passthrough",
                List.of(FieldMapping.of("payload", "payload", "passthrough", MappingLossiness.NONE))
        );
    }
}
