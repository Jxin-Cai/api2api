package com.api2api.domain.protocol.model;

import java.util.Objects;

/**
 * 协议字段映射明细。
 */
public final class FieldMapping {
    private final String sourceField;
    private final String targetField;
    private final String ruleDescription;
    private final MappingLossiness lossiness;

    private FieldMapping(String sourceField, String targetField, String ruleDescription, MappingLossiness lossiness) {
        this.sourceField = requireText(sourceField, "sourceField");
        this.targetField = requireText(targetField, "targetField");
        this.ruleDescription = requireText(ruleDescription, "ruleDescription");
        this.lossiness = Objects.requireNonNull(lossiness, "lossiness must not be null");
    }

    public static FieldMapping of(String sourceField, String targetField, String ruleDescription, MappingLossiness lossiness) {
        return new FieldMapping(sourceField, targetField, ruleDescription, lossiness);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ProtocolConversionException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public String sourceField() {
        return sourceField;
    }

    public String targetField() {
        return targetField;
    }

    public String ruleDescription() {
        return ruleDescription;
    }

    public MappingLossiness lossiness() {
        return lossiness;
    }
}
