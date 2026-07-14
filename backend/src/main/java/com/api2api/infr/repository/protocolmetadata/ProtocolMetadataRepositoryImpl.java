package com.api2api.infr.repository.protocolmetadata;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.FieldSection;
import com.api2api.domain.protocolmetadata.model.FieldType;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinition;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinitionId;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadataId;
import com.api2api.domain.protocolmetadata.model.UsageDirection;
import com.api2api.domain.protocolmetadata.repository.ProtocolMetadataRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProtocolMetadataRepositoryImpl implements ProtocolMetadataRepository {

    private static final Logger log = LoggerFactory.getLogger(ProtocolMetadataRepositoryImpl.class);
    private static final String CONFIG_PATTERN = "classpath:protocol-metadata/*.json";

    @NonNull
    private final ObjectMapper objectMapper;

    private final Map<ProtocolType, ProtocolMetadata> metadataByType = new LinkedHashMap<>();
    private final Map<ProtocolMetadataId, ProtocolMetadata> metadataById = new LinkedHashMap<>();

    @PostConstruct
    void loadFromConfig() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(CONFIG_PATTERN);
            long idCounter = 1;
            long fieldIdCounter = 1;

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Map<String, Object> raw = objectMapper.readValue(is, new TypeReference<>() {});
                    ProtocolMetadata metadata = parseMetadata(raw, idCounter, fieldIdCounter);
                    metadataByType.put(metadata.protocolType(), metadata);
                    metadataById.put(metadata.id(), metadata);
                    fieldIdCounter += metadata.fieldCount();
                    idCounter++;
                    log.info("Loaded protocol metadata: {} ({} fields)", metadata.protocolType(), metadata.fieldCount());
                } catch (IOException e) {
                    log.error("Failed to load protocol metadata from {}", resource.getFilename(), e);
                }
            }
            log.info("Loaded {} protocol metadata definitions", metadataByType.size());
        } catch (IOException e) {
            log.error("Failed to scan protocol metadata config files", e);
        }
    }

    @Override
    public List<ProtocolMetadata> findAll() {
        return new ArrayList<>(metadataByType.values());
    }

    @Override
    public Optional<ProtocolMetadata> findById(ProtocolMetadataId id) {
        return Optional.ofNullable(metadataById.get(id));
    }

    @Override
    public Optional<ProtocolMetadata> findByProtocolType(ProtocolType protocolType) {
        return Optional.ofNullable(metadataByType.get(protocolType));
    }

    @SuppressWarnings("unchecked")
    private ProtocolMetadata parseMetadata(Map<String, Object> raw, long metadataId, long startFieldId) {
        ProtocolType protocolType = ProtocolType.valueOf((String) raw.get("protocolType"));
        Instant now = Instant.now();

        List<Map<String, Object>> rawFields = (List<Map<String, Object>>) raw.get("fields");
        List<ProtocolFieldDefinition> fields = new ArrayList<>();
        long fieldId = startFieldId;
        int sortOrder = 1;

        for (Map<String, Object> rawField : rawFields) {
            fields.add(ProtocolFieldDefinition.of(
                    ProtocolFieldDefinitionId.of(fieldId++),
                    (String) rawField.get("fieldName"),
                    (String) rawField.get("fieldPath"),
                    FieldType.valueOf((String) rawField.get("fieldType")),
                    Boolean.TRUE.equals(rawField.get("required")),
                    FieldSection.valueOf((String) rawField.get("section")),
                    UsageDirection.valueOf((String) rawField.get("usageDirection")),
                    (String) rawField.get("description"),
                    (String) rawField.get("purpose"),
                    (String) rawField.get("usageContext"),
                    sortOrder++
            ));
        }

        return ProtocolMetadata.rehydrate(
                ProtocolMetadataId.of(metadataId),
                protocolType,
                (String) raw.get("displayName"),
                (String) raw.get("apiSpecVersion"),
                (String) raw.get("description"),
                (String) raw.get("defaultEndpointPath"),
                fields,
                now,
                now
        );
    }
}
