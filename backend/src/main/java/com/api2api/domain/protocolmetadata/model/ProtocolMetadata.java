package com.api2api.domain.protocolmetadata.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProtocolMetadata {
    private final ProtocolMetadataId id;
    private final ProtocolType protocolType;
    private final String displayName;
    private final String apiSpecVersion;
    private final String description;
    private final String defaultEndpointPath;
    private final List<ProtocolFieldDefinition> fieldDefinitions;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProtocolMetadata(
            ProtocolMetadataId id,
            ProtocolType protocolType,
            String displayName,
            String apiSpecVersion,
            String description,
            String defaultEndpointPath,
            List<ProtocolFieldDefinition> fieldDefinitions,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.protocolType = Objects.requireNonNull(protocolType, "protocolType must not be null");
        this.displayName = requireText(displayName, "displayName");
        this.apiSpecVersion = requireText(apiSpecVersion, "apiSpecVersion");
        this.description = description;
        this.defaultEndpointPath = requireText(defaultEndpointPath, "defaultEndpointPath");
        this.fieldDefinitions = fieldDefinitions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(fieldDefinitions));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ProtocolMetadata rehydrate(
            ProtocolMetadataId id,
            ProtocolType protocolType,
            String displayName,
            String apiSpecVersion,
            String description,
            String defaultEndpointPath,
            List<ProtocolFieldDefinition> fieldDefinitions,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ProtocolMetadata(id, protocolType, displayName, apiSpecVersion, description, defaultEndpointPath, fieldDefinitions, createdAt, updatedAt);
    }

    public Map<FieldSection, List<ProtocolFieldDefinition>> fieldsBySection() {
        return fieldDefinitions.stream()
                .collect(Collectors.groupingBy(ProtocolFieldDefinition::section));
    }

    public List<ProtocolFieldDefinition> inputFields() {
        return fieldDefinitions.stream()
                .filter(field -> field.usageDirection() == UsageDirection.INPUT || field.usageDirection() == UsageDirection.BOTH)
                .toList();
    }

    public List<ProtocolFieldDefinition> outputFields() {
        return fieldDefinitions.stream()
                .filter(field -> field.usageDirection() == UsageDirection.OUTPUT || field.usageDirection() == UsageDirection.BOTH)
                .toList();
    }

    public int fieldCount() {
        return fieldDefinitions.size();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ProtocolMetadataException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public ProtocolMetadataId id() {
        return id;
    }

    public ProtocolType protocolType() {
        return protocolType;
    }

    public String displayName() {
        return displayName;
    }

    public String apiSpecVersion() {
        return apiSpecVersion;
    }

    public String description() {
        return description;
    }

    public String defaultEndpointPath() {
        return defaultEndpointPath;
    }

    public List<ProtocolFieldDefinition> fieldDefinitions() {
        return fieldDefinitions;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
