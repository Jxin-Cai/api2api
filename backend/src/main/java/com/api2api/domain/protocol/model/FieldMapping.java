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
    private final String category;
    private final String mappingType;
    private final String sourcePath;
    private final String targetPath;
    private final String sourceType;
    private final String targetType;
    private final Boolean required;
    private final Boolean supported;
    private final String defaultValue;
    private final String condition;
    private final String notes;

    private FieldMapping(
            String sourceField,
            String targetField,
            String ruleDescription,
            MappingLossiness lossiness,
            String category,
            String mappingType,
            String sourcePath,
            String targetPath,
            String sourceType,
            String targetType,
            Boolean required,
            Boolean supported,
            String defaultValue,
            String condition,
            String notes
    ) {
        this.sourceField = requireText(sourceField, "sourceField");
        this.targetField = requireText(targetField, "targetField");
        this.ruleDescription = requireText(ruleDescription, "ruleDescription");
        this.lossiness = Objects.requireNonNull(lossiness, "lossiness must not be null");
        this.category = optionalText(category);
        this.mappingType = optionalText(mappingType);
        this.sourcePath = optionalText(sourcePath);
        this.targetPath = optionalText(targetPath);
        this.sourceType = optionalText(sourceType);
        this.targetType = optionalText(targetType);
        this.required = required;
        this.supported = supported;
        this.defaultValue = optionalText(defaultValue);
        this.condition = optionalText(condition);
        this.notes = optionalText(notes);
    }

    public static FieldMapping of(String sourceField, String targetField, String ruleDescription, MappingLossiness lossiness) {
        return new FieldMapping(
                sourceField,
                targetField,
                ruleDescription,
                lossiness,
                inferCategory(sourceField, targetField, ruleDescription),
                inferMappingType(sourceField, targetField, ruleDescription, lossiness),
                sourceField,
                targetField,
                null,
                null,
                null,
                true,
                null,
                null,
                null
        );
    }

    public static FieldMapping detailed(
            String sourceField,
            String targetField,
            String ruleDescription,
            MappingLossiness lossiness,
            String category,
            String mappingType,
            String sourcePath,
            String targetPath,
            String sourceType,
            String targetType,
            Boolean required,
            Boolean supported,
            String defaultValue,
            String condition,
            String notes
    ) {
        return new FieldMapping(sourceField, targetField, ruleDescription, lossiness, category, mappingType,
                sourcePath, targetPath, sourceType, targetType, required, supported, defaultValue, condition, notes);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ProtocolConversionException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String inferCategory(String sourceField, String targetField, String ruleDescription) {
        String combined = (sourceField + " " + targetField + " " + ruleDescription).toLowerCase();
        if (containsAny(combined, "tool", "function")) {
            return "TOOL";
        }
        if (containsAny(combined, "reasoning", "thinking")) {
            return "REASONING";
        }
        if (containsAny(combined, "usage", "token", "cache")) {
            return "USAGE";
        }
        if (containsAny(combined, "stream", "delta", "event")) {
            return "STREAMING";
        }
        if (containsAny(combined, "model", "temperature", "top_p", "top_k", "max_tokens", "max_output_tokens", "stop")) {
            return "MODEL";
        }
        if (containsAny(combined, "message", "content", "input", "output", "prompt", "choice")) {
            return "MESSAGE";
        }
        if (containsAny(combined, "metadata", "meta")) {
            return "METADATA";
        }
        return "OTHER";
    }

    private static String inferMappingType(String sourceField, String targetField, String ruleDescription, MappingLossiness lossiness) {
        String rule = ruleDescription.toLowerCase();
        if (lossiness != MappingLossiness.NONE) {
            return "DROP";
        }
        if (sourceField.equals(targetField) || containsAny(rule, "direct", "passthrough", "pass through", "直通", "透传")) {
            return "DIRECT";
        }
        if (containsAny(rule, "default", "fallback", "默认", "缺省", "兜底")) {
            return "DEFAULT";
        }
        if (containsAny(rule, "merge", "split", "flatten", "wrap", "unwrap", "重组", "合并", "拆分")) {
            return "RESHAPE";
        }
        if (containsAny(rule, "convert", "format", "parse", "normalize", "转换", "格式化", "归一化")) {
            return "TRANSFORM";
        }
        return "RENAME";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    public String category() {
        return category;
    }

    public String mappingType() {
        return mappingType;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public String targetPath() {
        return targetPath;
    }

    public String sourceType() {
        return sourceType;
    }

    public String targetType() {
        return targetType;
    }

    public Boolean required() {
        return required;
    }

    public Boolean supported() {
        return supported;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String condition() {
        return condition;
    }

    public String notes() {
        return notes;
    }
}
