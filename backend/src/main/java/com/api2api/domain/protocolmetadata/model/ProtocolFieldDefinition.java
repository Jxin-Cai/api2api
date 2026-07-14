package com.api2api.domain.protocolmetadata.model;

import java.util.Objects;

public final class ProtocolFieldDefinition {
    private final ProtocolFieldDefinitionId id;
    private final String fieldName;
    private final String fieldPath;
    private final FieldType fieldType;
    private final boolean required;
    private final FieldSection section;
    private final UsageDirection usageDirection;
    private final String description;
    private final String purpose;
    private final String usageContext;
    private final int sortOrder;

    private ProtocolFieldDefinition(
            ProtocolFieldDefinitionId id,
            String fieldName,
            String fieldPath,
            FieldType fieldType,
            boolean required,
            FieldSection section,
            UsageDirection usageDirection,
            String description,
            String purpose,
            String usageContext,
            int sortOrder
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.fieldName = requireText(fieldName, "fieldName");
        this.fieldPath = requireText(fieldPath, "fieldPath");
        this.fieldType = Objects.requireNonNull(fieldType, "fieldType must not be null");
        this.required = required;
        this.section = Objects.requireNonNull(section, "section must not be null");
        this.usageDirection = Objects.requireNonNull(usageDirection, "usageDirection must not be null");
        this.description = description;
        this.purpose = purpose;
        this.usageContext = usageContext;
        this.sortOrder = sortOrder;
    }

    public static ProtocolFieldDefinition of(
            ProtocolFieldDefinitionId id,
            String fieldName,
            String fieldPath,
            FieldType fieldType,
            boolean required,
            FieldSection section,
            UsageDirection usageDirection,
            String description,
            String purpose,
            String usageContext,
            int sortOrder
    ) {
        return new ProtocolFieldDefinition(id, fieldName, fieldPath, fieldType, required, section, usageDirection, description, purpose, usageContext, sortOrder);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ProtocolMetadataException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public ProtocolFieldDefinitionId id() {
        return id;
    }

    public String fieldName() {
        return fieldName;
    }

    public String fieldPath() {
        return fieldPath;
    }

    public FieldType fieldType() {
        return fieldType;
    }

    public boolean required() {
        return required;
    }

    public FieldSection section() {
        return section;
    }

    public UsageDirection usageDirection() {
        return usageDirection;
    }

    public String description() {
        return description;
    }

    public String purpose() {
        return purpose;
    }

    public String usageContext() {
        return usageContext;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
