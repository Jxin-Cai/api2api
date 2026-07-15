package com.api2api.infr.repository.protocol.converter;

import com.api2api.domain.channel.model.ProtocolType;
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
import com.api2api.infr.protocol.ProtocolConversionProgramRegistry;
import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProtocolConversionDefinitionPersistenceConverter {

    @NonNull
    private final ProtocolConversionProgramRegistry programRegistry;

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
                .requestMappingJson(definition.requestMapping().summary())
                .responseMappingJson(definition.responseMapping().summary())
                .implementationStatus(definition.implementationStatus().name())
                .createdTime(definition.createdAt())
                .updatedTime(definition.updatedAt())
                .build();
    }

    public ProtocolConversionDefinition toDomain(ProtocolConversionDefinitionPO po) {
        ProtocolType source = ProtocolType.valueOf(po.getSourceProtocol());
        ProtocolType target = ProtocolType.valueOf(po.getTargetProtocol());

        List<FieldMapping> requestMappings = programRegistry.describeRequestMappings(source, target)
                .orElse(List.of());
        List<FieldMapping> responseMappings = programRegistry.describeResponseMappings(source, target)
                .orElse(List.of());

        MappingDocument requestMapping = buildMappingDocument(
                MappingDirection.REQUEST, requestMappings, po.getRequestMappingJson());
        MappingDocument responseMapping = buildMappingDocument(
                MappingDirection.RESPONSE, responseMappings, po.getResponseMappingJson());

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

    private MappingDocument buildMappingDocument(
            MappingDirection direction, List<FieldMapping> programMappings, String summaryText) {
        String summary = normalizeSummary(summaryText);
        String title = direction == MappingDirection.REQUEST ? "Request mapping" : "Response mapping";

        if (!programMappings.isEmpty()) {
            return MappingDocument.of(direction, title, summary, programMappings);
        }
        return MappingDocument.of(direction, title, summary, fallbackMappings());
    }

    private List<FieldMapping> fallbackMappings() {
        return List.of(FieldMapping.of("payload", "payload",
                "Converter registered but field mappings not yet described", MappingLossiness.NONE));
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

    private String normalizeSummary(String text) {
        String summary = text == null || text.isBlank() ? "Mapping projected from executable converter." : text.trim();
        if (summary.length() > 1000) {
            summary = summary.substring(0, 1000);
        }
        return summary;
    }
}
