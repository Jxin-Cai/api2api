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
            new DefaultProtocolConversionServiceAdapter(List.of(), List.of(), new ObjectMapper());

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
