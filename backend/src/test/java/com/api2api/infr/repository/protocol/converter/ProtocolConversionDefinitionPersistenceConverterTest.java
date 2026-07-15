package com.api2api.infr.repository.protocol.converter;

import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.infr.protocol.ProtocolConversionProgramRegistry;
import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConversionDefinitionPersistenceConverterTest {

    private final ProtocolConversionProgramRegistry registry = new ProtocolConversionProgramRegistry(List.of());
    private final ProtocolConversionDefinitionPersistenceConverter converter =
            new ProtocolConversionDefinitionPersistenceConverter(registry);

    @Test
    void test_fallbackMapping_when_noConverterRegistered() {
        ProtocolConversionDefinition definition = converter.toDomain(po(
                "CLAUDE_MESSAGES",
                "OPENAI_RESPONSES",
                "Claude messages to OpenAI responses request mapping",
                "OpenAI responses to Claude messages response mapping"
        ));

        assertThat(definition.requestMapping().fieldMappings())
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.sourceField()).isEqualTo("payload");
                    assertThat(mapping.targetField()).isEqualTo("payload");
                    assertThat(mapping.ruleDescription()).contains("not yet described");
                });
    }

    @Test
    void test_passthroughFallback_when_sameProtocol() {
        ProtocolConversionDefinition definition = converter.toDomain(po(
                "CLAUDE_MESSAGES",
                "CLAUDE_MESSAGES",
                "Request passthrough",
                "Response passthrough"
        ));

        assertThat(definition.requestMapping().fieldMappings())
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.sourceField()).isEqualTo("payload");
                    assertThat(mapping.targetField()).isEqualTo("payload");
                });
    }

    private ProtocolConversionDefinitionPO po(
            String sourceProtocol,
            String targetProtocol,
            String requestMappingJson,
            String responseMappingJson
    ) {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        boolean passthrough = sourceProtocol.equals(targetProtocol);
        return ProtocolConversionDefinitionPO.builder()
                .id(1L)
                .sourceProtocol(sourceProtocol)
                .targetProtocol(targetProtocol)
                .kind(passthrough ? "PASSTHROUGH" : "TRANSFORM")
                .status("ENABLED")
                .implementationStatus("IMPLEMENTED")
                .supportsStreaming(false)
                .supportsToolCalling(false)
                .supportsReasoning(false)
                .supportsUsageMapping(true)
                .supportsCacheTokenMapping(true)
                .requestMappingJson(requestMappingJson)
                .responseMappingJson(responseMappingJson)
                .createdTime(now)
                .updatedTime(now)
                .build();
    }
}
