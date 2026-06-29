package com.api2api.domain.protocol.model;

import java.util.List;

/**
 * 设计文档中的映射元数据别名，继承 MappingDocument 的展示语义。
 */
public final class MappingMetadata {
    private final MappingDocument document;

    private MappingMetadata(MappingDocument document) {
        this.document = document;
    }

    public static MappingMetadata of(MappingDirection direction, String title, String summary, List<FieldMapping> fieldMappings) {
        return new MappingMetadata(MappingDocument.of(direction, title, summary, fieldMappings));
    }

    public MappingMetadata append(FieldMapping fieldMapping) {
        return new MappingMetadata(document.append(fieldMapping));
    }

    public MappingDocument toDocument() {
        return document;
    }

    public MappingDirection direction() {
        return document.direction();
    }

    public String title() {
        return document.title();
    }

    public String summary() {
        return document.summary();
    }

    public List<FieldMapping> fieldMappings() {
        return document.fieldMappings();
    }
}
