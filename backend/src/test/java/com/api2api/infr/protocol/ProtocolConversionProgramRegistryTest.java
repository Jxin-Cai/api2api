package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.FieldMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConversionProgramRegistryTest {

    private final List<ProtocolMessageConverter> allConverters = createAllConverters();
    private final ProtocolConversionProgramRegistry registry = new ProtocolConversionProgramRegistry(allConverters);

    @Test
    void test_registryIndexesAllConverters_when_allFourteenRegistered() {
        assertThat(allConverters).hasSize(14);
    }

    @ParameterizedTest(name = "{0}→{1} {2}")
    @MethodSource("allConverterDirections")
    void test_converterHasNonEmptyFieldMappings_when_registeredInRegistry(
            ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {

        var mappings = registry.findProgram(source, target, direction)
                .map(com.api2api.infr.protocol.conversion.ProtocolConversionProgram::fieldMappings);

        assertThat(mappings)
                .as("%s→%s %s executable converter must expose its own field mappings", source, target, direction)
                .isPresent()
                .hasValueSatisfying(list -> {
                    assertThat(list).isNotEmpty();
                    assertThat(list)
                            .noneMatch(mapping -> mapping.sourceField().equals("payload")
                                    && mapping.targetField().equals("payload"));
                });
    }

    @ParameterizedTest(name = "{0}→{1} {2}")
    @MethodSource("allConverterDirections")
    void test_everyMappingHasRequiredFields_when_descriptionProvided(
            ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {

        var mappings = registry.findProgram(source, target, direction)
                .map(com.api2api.infr.protocol.conversion.ProtocolConversionProgram::fieldMappings);

        mappings.ifPresent(list -> list.forEach(mapping -> {
            assertThat(mapping.sourceField()).as("sourceField").isNotBlank();
            assertThat(mapping.targetField()).as("targetField").isNotBlank();
            assertThat(mapping.ruleDescription()).as("ruleDescription").isNotBlank();
            assertThat(mapping.lossiness()).as("lossiness").isNotNull();
        }));
    }

    @Test
    void test_marksDroppedFieldUnmapped_when_targetHasNoEquivalentField() {
        // Arrange
        List<FieldMapping> mappings = ConverterFieldMappingDescriptions.lookup(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.OPENAI_RESPONSES,
                ProtocolConversionDirection.REQUEST
        ).orElseThrow();

        // Act
        FieldMapping topK = mappings.stream()
                .filter(mapping -> mapping.sourceField().equals("top_k"))
                .findFirst()
                .orElseThrow();

        // Assert
        assertThat(topK.supported()).isFalse();
    }

    @Test
    void test_exposesLeafFieldMapping_when_toolDefinitionContainsMultipleFields() {
        // Arrange
        List<FieldMapping> mappings = ConverterFieldMappingDescriptions.lookup(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.OPENAI_RESPONSES,
                ProtocolConversionDirection.REQUEST
        ).orElseThrow();

        // Act
        FieldMapping inputSchema = mappings.stream()
                .filter(mapping -> mapping.sourceField().equals("tools[].input_schema"))
                .findFirst()
                .orElseThrow();

        // Assert
        assertThat(inputSchema.targetField()).isEqualTo("tools[].parameters");
    }

    @Test
    void test_exposesRoundTripMappings_when_claudeRoutesToBedrockMessages() {
        // Arrange / Act
        List<FieldMapping> requestMappings = registry.describeRequestMappings(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES
        ).orElseThrow();
        List<FieldMapping> responseMappings = registry.describeResponseMappings(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES
        ).orElseThrow();

        // Assert
        assertThat(requestMappings)
                .anyMatch(mapping -> mapping.sourceField().equals("model")
                        && mapping.targetField().equals("URI path {modelId}"));
        assertThat(responseMappings)
                .anyMatch(mapping -> mapping.sourceField().equals("AWS application/vnd.amazon.eventstream")
                        && mapping.targetField().equals("Claude SSE events"));
    }

    @Test
    void test_usesReverseExecutableDirection_when_describingRouteResponse() {
        // Arrange / Act
        List<FieldMapping> responseMappings = registry.describeResponseMappings(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.OPENAI_RESPONSES
        ).orElseThrow();

        // Assert
        assertThat(responseMappings)
                .anyMatch(mapping -> mapping.sourceField().equals("output[].content (text)")
                        && mapping.targetField().equals("content[].type=text"));
    }

    static Stream<Arguments> allConverterDirections() {
        return Stream.of(
                // Generic converters (12)
                Arguments.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE),
                Arguments.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE),
                Arguments.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE),
                Arguments.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE),
                Arguments.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE),
                Arguments.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE),
                // Bedrock Claude Messages converters (2)
                Arguments.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE)
        );
    }

    private static List<ProtocolMessageConverter> createAllConverters() {
        return allConverterDirections()
                .map(args -> stubConverter(
                        (ProtocolType) args.get()[0],
                        (ProtocolType) args.get()[1],
                        (ProtocolConversionDirection) args.get()[2]))
                .toList();
    }

    private static ProtocolMessageConverter stubConverter(
            ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {
        return new ProtocolMessageConverter() {
            @Override public ProtocolType sourceProtocol() { return source; }
            @Override public ProtocolType targetProtocol() { return target; }
            @Override public ProtocolConversionDirection direction() { return direction; }
            @Override public com.api2api.domain.protocol.model.ConversionCapability capability() { return null; }
            @Override public com.api2api.domain.protocol.model.ProtocolConversionResult convert(
                    com.api2api.domain.protocol.model.ProtocolPayload p,
                    com.api2api.domain.protocol.model.ProtocolConversionRequest r) { return null; }
            @Override public com.api2api.infr.protocol.conversion.ProtocolConversionProgram conversionProgram() {
                return com.api2api.infr.protocol.conversion.ProtocolConversionProgram.singleRule(
                        source,
                        target,
                        direction,
                        "test converter",
                        (sourceNode, requirement) -> sourceNode,
                        ConverterFieldMappingDescriptions.lookup(source, target, direction).orElse(List.of())
                );
            }
        };
    }
}
