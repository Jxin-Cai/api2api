package com.api2api.infr.protocol.contract;

import com.api2api.domain.protocolcontract.model.ProtocolContractViolationException;
import com.api2api.domain.protocolmetadata.model.FieldSection;
import com.api2api.domain.protocolmetadata.model.FieldType;
import com.api2api.domain.protocolmetadata.model.UsageDirection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A metadata-bearing field reference that also performs runtime JSON access. */
public final class ProtocolFieldRef {

    private final String fieldName;
    private final String path;
    private final FieldType type;
    private final boolean required;
    private final FieldSection section;
    private final UsageDirection direction;
    private final String description;
    private final String purpose;
    private final String usageContext;

    private ProtocolFieldRef(
            String fieldName,
            String path,
            FieldType type,
            boolean required,
            FieldSection section,
            UsageDirection direction,
            String description,
            String purpose,
            String usageContext
    ) {
        this.fieldName = requireText(fieldName, "fieldName");
        this.path = requireText(path, "path");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.required = required;
        this.section = Objects.requireNonNull(section, "section must not be null");
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.description = description;
        this.purpose = purpose;
        this.usageContext = usageContext;
    }

    public static ProtocolFieldRef of(
            String fieldName,
            String path,
            FieldType type,
            boolean required,
            FieldSection section,
            UsageDirection direction,
            String description,
            String purpose,
            String usageContext
    ) {
        return new ProtocolFieldRef(fieldName, path, type, required, section, direction,
                description, purpose, usageContext);
    }

    static ProtocolFieldRef runtime(String path, FieldType type) {
        return new ProtocolFieldRef(path, path, type, false, FieldSection.OTHER,
                UsageDirection.BOTH, null, null, null);
    }

    public JsonNode read(JsonNode root, ProtocolShapeKind kind) {
        List<JsonNode> values = readAll(root, kind);
        return values.isEmpty() ? MissingNode.getInstance() : values.get(0);
    }

    public List<JsonNode> readAll(JsonNode root, ProtocolShapeKind kind) {
        Objects.requireNonNull(root, "root must not be null");
        String resolvedPath = normalizedPath(root, kind);
        String expectedValue = null;
        int equalsIndex = resolvedPath.lastIndexOf('=');
        if (equalsIndex >= 0) {
            expectedValue = resolvedPath.substring(equalsIndex + 1);
            resolvedPath = resolvedPath.substring(0, equalsIndex);
        }
        List<JsonNode> current = List.of(root);
        if (!resolvedPath.isEmpty()) {
            for (String segment : resolvedPath.split("\\.")) {
                boolean array = segment.endsWith("[]");
                String property = array ? segment.substring(0, segment.length() - 2) : segment;
                List<JsonNode> next = new ArrayList<>();
                for (JsonNode node : current) {
                    JsonNode value = property.isEmpty() ? node : node.get(property);
                    if (value == null || value.isNull() || value.isMissingNode()) {
                        continue;
                    }
                    if (array) {
                        if (value.isArray()) {
                            value.forEach(next::add);
                        }
                    } else {
                        next.add(value);
                    }
                }
                current = List.copyOf(next);
            }
        }
        if (current.isEmpty() && kind == ProtocolShapeKind.STREAM_EVENT) {
            for (String container : List.of("response", "message", "metadata")) {
                JsonNode nested = root.get(container);
                if (nested != null && nested.isObject()) {
                    List<JsonNode> nestedValues = readAll(nested, ProtocolShapeKind.RESPONSE);
                    if (!nestedValues.isEmpty()) {
                        current = nestedValues;
                        break;
                    }
                }
            }
        }
        if (expectedValue == null) {
            return current;
        }
        String expected = expectedValue;
        return current.stream().filter(JsonNode::isTextual)
                .filter(node -> expected.equals(node.asText())).toList();
    }

    public boolean present(JsonNode root, ProtocolShapeKind kind) {
        return !readAll(root, kind).isEmpty();
    }

    public boolean presentAndNotEmpty(JsonNode root, ProtocolShapeKind kind) {
        return readAll(root, kind).stream().anyMatch(value -> !value.isArray() || !value.isEmpty());
    }

    public String readText(JsonNode root, ProtocolShapeKind kind) {
        JsonNode value = read(root, kind);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw violation("must be a non-blank string");
        }
        return value.asText();
    }

    public boolean readBoolean(JsonNode root, ProtocolShapeKind kind, boolean defaultValue) {
        JsonNode value = read(root, kind);
        return value.isBoolean() ? value.asBoolean() : defaultValue;
    }

    public long readLong(JsonNode root, ProtocolShapeKind kind, long defaultValue) {
        JsonNode value = read(root, kind);
        return value.isIntegralNumber() ? value.asLong() : defaultValue;
    }

    public void write(ObjectNode root, JsonNode value, ProtocolShapeKind kind) {
        String resolvedPath = normalizedPath(root, kind);
        if (resolvedPath.contains("[]") || resolvedPath.contains("=")) {
            throw violation("cannot be written without an array element context");
        }
        String[] segments = resolvedPath.split("\\.");
        ObjectNode parent = root;
        for (int i = 0; i < segments.length - 1; i++) {
            JsonNode child = parent.get(segments[i]);
            parent = child instanceof ObjectNode objectNode
                    ? objectNode
                    : parent.putObject(segments[i]);
        }
        parent.set(segments[segments.length - 1], Objects.requireNonNull(value, "value must not be null"));
    }

    void validate(JsonNode root, ProtocolShapeKind kind, boolean enforceRequired) {
        List<JsonNode> values = readAll(root, kind);
        if (values.isEmpty()) {
            if (enforceRequired && required && isRootField(root, kind)) {
                throw violation("is required");
            }
            return;
        }
        for (JsonNode value : values) {
            if (!matchesType(value)) {
                throw violation("must have type " + type);
            }
        }
    }

    private boolean matchesType(JsonNode value) {
        boolean primary = switch (type) {
            case STRING, ENUM -> value.isTextual();
            case INTEGER -> value.isIntegralNumber();
            case FLOAT -> value.isNumber();
            case BOOLEAN -> value.isBoolean();
            case ARRAY -> value.isArray();
            case OBJECT -> value.isObject();
        };
        if (primary) {
            return true;
        }
        String context = usageContext == null ? "" : usageContext;
        if (type == FieldType.ARRAY && context.contains("字符串") && value.isTextual()) {
            return true;
        }
        if ((type == FieldType.STRING || type == FieldType.ENUM)
                && context.contains("数组") && value.isArray()) {
            return true;
        }
        return type == FieldType.OBJECT && context.contains("可为 auto") && value.isTextual();
    }

    private boolean isRootField(JsonNode root, ProtocolShapeKind kind) {
        String normalized = normalizedPath(root, kind);
        return !normalized.contains(".") && !normalized.contains("[]") && !normalized.contains("=");
    }

    private String normalizedPath(JsonNode root, ProtocolShapeKind kind) {
        String normalized = path;
        if (kind == ProtocolShapeKind.RESPONSE && normalized.startsWith("response.")) {
            normalized = normalized.substring("response.".length());
        }
        if (kind == ProtocolShapeKind.REQUEST && normalized.startsWith("content[]")) {
            normalized = "messages[].content[]" + normalized.substring("content[]".length());
        }
        if (kind == ProtocolShapeKind.STREAM_EVENT && normalized.startsWith("stream.event.")) {
            String eventType = normalized.substring("stream.event.".length());
            return eventType.equals(root.path("type").asText()) ? "type" : "__missing_event__";
        }
        if (kind == ProtocolShapeKind.STREAM_EVENT && normalized.startsWith("streaming.")) {
            String eventType = normalized.substring("streaming.".length());
            if (eventType.equals(root.path("type").asText())) {
                return "type";
            }
            return root.has(eventType) ? eventType : "__missing_event__";
        }
        return normalized;
    }

    private ProtocolContractViolationException violation(String detail) {
        return new ProtocolContractViolationException("Protocol field '" + path + "' " + detail);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public String fieldName() { return fieldName; }
    public String path() { return path; }
    public FieldType type() { return type; }
    public boolean required() { return required; }
    public FieldSection section() { return section; }
    public UsageDirection direction() { return direction; }
    public String description() { return description; }
    public String purpose() { return purpose; }
    public String usageContext() { return usageContext; }
}
