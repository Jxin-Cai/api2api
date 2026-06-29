package com.api2api.domain.protocol.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 请求或响应字段映射展示文档。
 */
public final class MappingDocument {
    private static final int TITLE_MAX_LENGTH = 120;
    private static final int SUMMARY_MAX_LENGTH = 1000;

    private final MappingDirection direction;
    private final String title;
    private final String summary;
    private final List<FieldMapping> fieldMappings;

    private MappingDocument(MappingDirection direction, String title, String summary, List<FieldMapping> fieldMappings) {
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.title = requireText(title, "title", TITLE_MAX_LENGTH);
        this.summary = requireText(summary, "summary", SUMMARY_MAX_LENGTH);
        Objects.requireNonNull(fieldMappings, "fieldMappings must not be null");
        if (fieldMappings.isEmpty()) {
            throw new ProtocolConversionException("fieldMappings must not be empty");
        }
        fieldMappings.forEach(mapping -> Objects.requireNonNull(mapping, "fieldMapping must not be null"));
        this.fieldMappings = Collections.unmodifiableList(new ArrayList<>(fieldMappings));
    }

    public static MappingDocument of(MappingDirection direction, String title, String summary, List<FieldMapping> fieldMappings) {
        return new MappingDocument(direction, title, summary, fieldMappings);
    }

    public MappingDocument append(FieldMapping fieldMapping) {
        Objects.requireNonNull(fieldMapping, "fieldMapping must not be null");
        List<FieldMapping> appended = new ArrayList<>(fieldMappings);
        appended.add(fieldMapping);
        return new MappingDocument(direction, title, summary, appended);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ProtocolConversionException(fieldName + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ProtocolConversionException(fieldName + " length must be less than or equal to " + maxLength);
        }
        return trimmed;
    }

    public MappingDirection direction() {
        return direction;
    }

    public String title() {
        return title;
    }

    public String summary() {
        return summary;
    }

    public List<FieldMapping> fieldMappings() {
        return fieldMappings;
    }
}
