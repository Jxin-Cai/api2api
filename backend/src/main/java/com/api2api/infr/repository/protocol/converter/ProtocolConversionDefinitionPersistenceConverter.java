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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProtocolConversionDefinitionPersistenceConverter {

    private static final Logger log = LoggerFactory.getLogger(ProtocolConversionDefinitionPersistenceConverter.class);
    private static final String MAPPING_CONFIG_PATTERN = "classpath:protocol-conversion-mappings/*.json";

    @NonNull
    private final ObjectMapper objectMapper;

    private final Map<String, MappingConfigEntry> mappingConfigs = new HashMap<>();

    @PostConstruct
    void loadMappingConfigs() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(MAPPING_CONFIG_PATTERN);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Map<String, Object> raw = objectMapper.readValue(is, new TypeReference<>() {});
                    String source = (String) raw.get("sourceProtocol");
                    String target = (String) raw.get("targetProtocol");
                    String key = source + "_TO_" + target;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> requestMappings = (List<Map<String, Object>>) raw.get("requestMappings");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> responseMappings = (List<Map<String, Object>>) raw.get("responseMappings");
                    mappingConfigs.put(key, new MappingConfigEntry(
                            parseDetailedMappings(requestMappings),
                            parseDetailedMappings(responseMappings)
                    ));
                    log.info("Loaded conversion mapping config: {} → {} ({} request, {} response mappings)",
                            source, target,
                            requestMappings != null ? requestMappings.size() : 0,
                            responseMappings != null ? responseMappings.size() : 0);
                } catch (IOException e) {
                    log.error("Failed to load conversion mapping config from {}", resource.getFilename(), e);
                }
            }
            log.info("Loaded {} conversion mapping configs", mappingConfigs.size());
        } catch (IOException e) {
            log.error("Failed to scan conversion mapping config files", e);
        }
    }

    private List<FieldMapping> parseDetailedMappings(List<Map<String, Object>> rawMappings) {
        if (rawMappings == null || rawMappings.isEmpty()) {
            return List.of();
        }
        List<FieldMapping> result = new ArrayList<>();
        for (Map<String, Object> raw : rawMappings) {
            String sourceField = (String) raw.get("sourceField");
            String targetField = (String) raw.getOrDefault("targetField", "(dropped)");
            String ruleDescription = (String) raw.getOrDefault("ruleDescription", "");
            String lossinessStr = (String) raw.getOrDefault("lossiness", "NONE");
            MappingLossiness lossiness = MappingLossiness.valueOf(lossinessStr);
            result.add(FieldMapping.detailed(
                    sourceField,
                    targetField,
                    ruleDescription,
                    lossiness,
                    (String) raw.get("category"),
                    (String) raw.get("mappingType"),
                    (String) raw.get("sourcePath"),
                    (String) raw.get("targetPath"),
                    (String) raw.get("sourceType"),
                    (String) raw.get("targetType"),
                    raw.get("required") instanceof Boolean b ? b : null,
                    raw.get("supported") instanceof Boolean b ? b : null,
                    (String) raw.get("defaultValue"),
                    (String) raw.get("condition"),
                    (String) raw.get("notes")
            ));
        }
        return result;
    }

    private record MappingConfigEntry(List<FieldMapping> requestMappings, List<FieldMapping> responseMappings) {}

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
        String configKey = source.name() + "_TO_" + target.name();
        MappingConfigEntry config = mappingConfigs.get(configKey);

        MappingDocument requestMapping;
        MappingDocument responseMapping;
        if (config != null && !config.requestMappings().isEmpty()) {
            String summary = normalizeSummary(po.getRequestMappingJson());
            requestMapping = MappingDocument.of(MappingDirection.REQUEST, "Request mapping", summary, config.requestMappings());
        } else {
            requestMapping = toMapping(MappingDirection.REQUEST, po.getRequestMappingJson());
        }
        if (config != null && !config.responseMappings().isEmpty()) {
            String summary = normalizeSummary(po.getResponseMappingJson());
            responseMapping = MappingDocument.of(MappingDirection.RESPONSE, "Response mapping", summary, config.responseMappings());
        } else {
            responseMapping = toMapping(MappingDirection.RESPONSE, po.getResponseMappingJson());
        }

        return ProtocolConversionDefinition.rehydrate(
                ProtocolConversionDefinitionId.of(po.getId()),
                source,
                target,
                toCapability(po),
                requestMapping,
                responseMapping,
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
            MappingLossiness lossiness = inferLossiness(source, target, sources.size(), targets.size(), index);
            mappings.add(FieldMapping.of(source, target, describeRule(source, target, lossiness), lossiness));
        }
    }

    private MappingLossiness inferLossiness(String source, String target, int sourceCount, int targetCount, int index) {
        if (sourceCount > targetCount && index >= targetCount) {
            return MappingLossiness.LOSSY;
        }
        if (targetCount > sourceCount && index >= sourceCount) {
            return MappingLossiness.PARTIAL;
        }
        if (source.equals(target)) {
            return MappingLossiness.NONE;
        }
        return MappingLossiness.PARTIAL;
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

    private String describeRule(String source, String target, MappingLossiness lossiness) {
        if (lossiness == MappingLossiness.LOSSY) {
            return source + " → dropped (no equivalent in target protocol)";
        }
        if (source.equals(target)) {
            return "Direct passthrough";
        }
        return "Restructure " + source + " → " + target;
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
