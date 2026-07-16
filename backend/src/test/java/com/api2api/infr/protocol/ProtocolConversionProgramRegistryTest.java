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
    void test_registryIndexesAllConverters_when_allTwentyRegistered() {
        assertThat(allConverters).hasSize(20);
    }

    @ParameterizedTest(name = "{0}→{1} {2}")
    @MethodSource("allConverterDirections")
    void test_converterHasNonEmptyFieldMappings_when_registeredInRegistry(
            ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {

        var mappings = direction == ProtocolConversionDirection.REQUEST
                ? registry.describeRequestMappings(source, target)
                : registry.describeResponseMappings(source, target);

        assertThat(mappings)
                .as("%s→%s %s should have field mapping descriptions (same-source invariant)", source, target, direction)
                .isPresent()
                .hasValueSatisfying(list -> assertThat(list).isNotEmpty());
    }

    @ParameterizedTest(name = "{0}→{1} {2}")
    @MethodSource("allConverterDirections")
    void test_everyMappingHasRequiredFields_when_descriptionProvided(
            ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {

        var mappings = direction == ProtocolConversionDirection.REQUEST
                ? registry.describeRequestMappings(source, target)
                : registry.describeResponseMappings(source, target);

        mappings.ifPresent(list -> list.forEach(mapping -> {
            assertThat(mapping.sourceField()).as("sourceField").isNotBlank();
            assertThat(mapping.targetField()).as("targetField").isNotBlank();
            assertThat(mapping.ruleDescription()).as("ruleDescription").isNotBlank();
            assertThat(mapping.lossiness()).as("lossiness").isNotNull();
        }));
    }

    @Test
    void test_registryReturnsEmpty_when_noConverterForDirection() {
        assertThat(registry.describeRequestMappings(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES))
                .isEmpty();
    }

    @Test
    void test_marks_programmatic_tool_calling_unsupported_when_target_is_bedrock_converse() {
        // Arrange
        List<FieldMapping> mappings = ConverterFieldMappingDescriptions.lookup(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST
        ).orElseThrow();

        // Act
        FieldMapping allowedCallers = mappings.stream()
                .filter(mapping -> mapping.sourceField().equals("tools[].allowed_callers"))
                .findFirst()
                .orElseThrow();

        // Assert
        assertThat(allowedCallers.supported()).isFalse();
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
                // Bedrock converters (6)
                Arguments.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE),
                Arguments.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE),
                Arguments.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST),
                Arguments.of(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE),
                // Native Bedrock Claude Messages converters (2)
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
