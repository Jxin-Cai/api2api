package com.api2api.infr.repository.protocol.converter;

import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ConversionImplementationStatus;
import com.api2api.domain.protocol.model.ConversionStatus;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingDirection;
import com.api2api.domain.protocol.model.MappingDocument;
import com.api2api.domain.protocol.model.MappingLossiness;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Converts protocol conversion definition aggregate to persistence object.
 */
@Component
public class ProtocolConversionDefinitionPersistenceConverter {

    public ProtocolConversionDefinitionPO toPO(ProtocolConversionDefinition definition) {
        return ProtocolConversionDefinitionPO.builder()
                .id(definition.id().value())
                .sourceProtocol(definition.sourceProtocol().name())
                .targetProtocol(definition.targetProtocol().name())
                .kind(definition.kind().name())
                .status(definition.status().name())
                .supportsStreaming(definition.capability().supportsStreaming())
                .supportsToolCalling(definition.capability().supportsToolCalling())
                .supportsReasoning(definition.capability().supportsReasoning())
                .supportsUsageMapping(definition.capability().supportsUsageMapping())
                .supportsCacheTokenMapping(definition.capability().supportsCacheTokenMapping())
                .requestMappingJson(toMappingText(definition.requestMapping()))
                .responseMappingJson(toMappingText(definition.responseMapping()))
                .implementationStatus(definition.implementationStatus().name())
                .createdTime(definition.createdAt())
                .updatedTime(definition.updatedAt())
                .build();
    }

    public ProtocolConversionDefinition toDomain(ProtocolConversionDefinitionPO po) {
        ProtocolType source = ProtocolType.valueOf(po.getSourceProtocol());
        ProtocolType target = ProtocolType.valueOf(po.getTargetProtocol());
        return ProtocolConversionDefinition.rehydrate(
                ProtocolConversionDefinitionId.of(po.getId()),
                source,
                target,
                toCapability(po),
                toMapping(MappingDirection.REQUEST, po.getRequestMappingJson()),
                toMapping(MappingDirection.RESPONSE, po.getResponseMappingJson()),
                ConversionImplementationStatus.valueOf(po.getImplementationStatus()),
                ConversionStatus.valueOf(po.getStatus()),
                po.getCreatedTime(),
                po.getUpdatedTime()
        );
    }

    private ConversionCapability toCapability(ProtocolConversionDefinitionPO po) {
        Set<ContentMappingType> contentTypes = EnumSet.of(ContentMappingType.TEXT);
        if (po.isSupportsToolCalling()) {
            contentTypes.add(ContentMappingType.TOOL_CALL);
        }
        if (po.isSupportsReasoning()) {
            contentTypes.add(ContentMappingType.REASONING);
        }
        if (po.isSupportsUsageMapping()) {
            contentTypes.add(ContentMappingType.USAGE);
        }
        if (po.isSupportsCacheTokenMapping()) {
            contentTypes.add(ContentMappingType.CACHE_TOKENS);
        }
        if (po.isSupportsStreaming()) {
            contentTypes.add(ContentMappingType.STREAM_EVENT);
        }
        return ConversionCapability.of(
                po.isSupportsStreaming(),
                po.isSupportsToolCalling(),
                po.isSupportsReasoning(),
                po.isSupportsUsageMapping(),
                po.isSupportsCacheTokenMapping(),
                contentTypes
        );
    }

    private MappingDocument toMapping(MappingDirection direction, String text) {
        String summary = normalizeSummary(text);
        return MappingDocument.of(
                direction,
                direction == MappingDirection.REQUEST ? "Request mapping" : "Response mapping",
                summary,
                toFieldMappings(summary)
        );
    }

    private List<FieldMapping> toFieldMappings(String summary) {
        String[] clauses = summary.split("[,;\\n]+");
        List<FieldMapping> mappings = new ArrayList<>();
        for (String clause : clauses) {
            addFieldMappings(clause, mappings);
        }
        return mappings.isEmpty()
                ? List.of(FieldMapping.of("payload", "payload", "MVP passthrough or documented mapping", MappingLossiness.NONE))
                : mappings;
    }

    private void addFieldMappings(String clause, List<FieldMapping> mappings) {
        String trimmed = clause == null ? "" : clause.trim();
        if (trimmed.isBlank()) {
            return;
        }
        String[] sides = trimmed.split("\\s*->\\s*", 2);
        if (sides.length != 2) {
            mappings.add(FieldMapping.of("payload", "payload", trimmed, MappingLossiness.NONE));
            return;
        }
        List<String> sources = splitFieldList(sides[0]);
        List<String> targets = splitFieldList(sides[1]);
        int max = Math.max(sources.size(), targets.size());
        for (int index = 0; index < max; index++) {
            String source = fieldAt(sources, index);
            String target = fieldAt(targets, index);
            mappings.add(FieldMapping.of(source, target, describeRule(source, target), MappingLossiness.NONE));
        }
    }

    private List<String> splitFieldList(String text) {
        String[] fields = text.split("/");
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            String normalized = normalizeFieldPath(field);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result.isEmpty() ? List.of("payload") : result;
    }

    private String fieldAt(List<String> fields, int index) {
        if (fields.size() == 1) {
            return fields.get(0);
        }
        if (index < fields.size()) {
            return fields.get(index);
        }
        return fields.get(fields.size() - 1);
    }

    private String normalizeFieldPath(String field) {
        return normalizeKnownFieldName(field.trim().toLowerCase())
                .replaceAll("[^a-z0-9_\\[\\].*]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String normalizeKnownFieldName(String field) {
        return switch (field) {
            case "claude messages" -> "messages";
            case "claude messages content" -> "content";
            case "openai responses input" -> "input";
            case "openai responses output" -> "output";
            case "openai chat messages" -> "messages";
            case "openai chat choices" -> "choices";
            default -> field;
        };
    }

    private String describeRule(String source, String target) {
        if (source.equals(target)) {
            return "Direct passthrough";
        }
        return "Map " + source + " to " + target;
    }

    private String normalizeSummary(String text) {
        String summary = text == null || text.isBlank() ? "Mapping metadata is not configured." : text.trim();
        if (summary.length() > 1000) {
            summary = summary.substring(0, 1000);
        }
        return summary;
    }

    private String toMappingText(MappingDocument mapping) {
        return mapping.summary();
    }
}
