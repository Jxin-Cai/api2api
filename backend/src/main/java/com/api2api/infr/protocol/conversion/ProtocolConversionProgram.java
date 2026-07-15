package com.api2api.infr.protocol.conversion;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.infr.protocol.ProtocolConversionDirection;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;

public final class ProtocolConversionProgram {

    private final ProtocolType sourceProtocol;
    private final ProtocolType targetProtocol;
    private final ProtocolConversionDirection direction;
    private final List<ExecutableConversionRule> rules;

    private ProtocolConversionProgram(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionDirection direction,
            List<ExecutableConversionRule> rules
    ) {
        this.sourceProtocol = Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null");
        this.targetProtocol = Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.isEmpty()) {
            throw new ProtocolConversionException("conversion program must contain at least one executable rule");
        }
        rules.forEach(rule -> Objects.requireNonNull(rule, "rule must not be null"));
        this.rules = List.copyOf(rules);
    }

    public static ProtocolConversionProgram singleRule(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionDirection direction,
            String ruleName,
            JsonConversionOperation operation,
            List<FieldMapping> fieldMappings
    ) {
        return new ProtocolConversionProgram(
                sourceProtocol,
                targetProtocol,
                direction,
                List.of(new ConverterBackedConversionRule(ruleName, operation, fieldMappings))
        );
    }

    public JsonNode execute(JsonNode source, ProtocolConversionRequest requirement) {
        JsonNode current = Objects.requireNonNull(source, "source must not be null");
        ConversionRuleContext context = new ConversionRuleContext(current, requirement);
        for (ExecutableConversionRule rule : rules) {
            current = rule.execute(context);
            context = new ConversionRuleContext(current, requirement);
        }
        return current;
    }

    public List<FieldMapping> fieldMappings() {
        return rules.stream()
                .flatMap(rule -> rule.toFieldMappings().stream())
                .toList();
    }

    public ProtocolType sourceProtocol() {
        return sourceProtocol;
    }

    public ProtocolType targetProtocol() {
        return targetProtocol;
    }

    public ProtocolConversionDirection direction() {
        return direction;
    }

    public List<ExecutableConversionRule> rules() {
        return rules;
    }

    private record ConverterBackedConversionRule(
            String ruleName,
            JsonConversionOperation operation,
            List<FieldMapping> fieldMappings
    ) implements ExecutableConversionRule {

        private ConverterBackedConversionRule {
            if (ruleName == null || ruleName.isBlank()) {
                throw new ProtocolConversionException("ruleName must not be blank");
            }
            Objects.requireNonNull(operation, "operation must not be null");
            Objects.requireNonNull(fieldMappings, "fieldMappings must not be null");
            if (fieldMappings.isEmpty()) {
                throw new ProtocolConversionException("fieldMappings must not be empty");
            }
            fieldMappings.forEach(mapping -> Objects.requireNonNull(mapping, "fieldMapping must not be null"));
            fieldMappings = List.copyOf(fieldMappings);
        }

        @Override
        public JsonNode execute(ConversionRuleContext context) {
            return operation.convert(context.source(), context.requirement());
        }

        @Override
        public List<FieldMapping> toFieldMappings() {
            return fieldMappings;
        }
    }
}
