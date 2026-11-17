package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Checks strict-mode shape without changing optional properties into required arguments. */
final class ResponsesStructuredOutputSchema {
    private static final List<String> MAP_KEYWORDS = List.of("properties", "$defs", "definitions");
    private static final List<String> ARRAY_KEYWORDS = List.of("anyOf", "oneOf", "allOf", "prefixItems");
    private static final List<String> SCHEMA_KEYWORDS = List.of("items", "additionalProperties", "not");

    private ResponsesStructuredOutputSchema() {}

    static boolean isStrictCompatible(JsonNode schema) {
        return "object".equals(schema.path("type").asText()) && !schema.has("anyOf") && isCompatibleNode(schema);
    }

    private static boolean isCompatibleNode(JsonNode schema) {
        if (!schema.isObject()) {
            return false;
        }
        if (isObjectSchema(schema)) {
            JsonNode additional = schema.get("additionalProperties");
            if (additional != null && !additional.equals(com.fasterxml.jackson.databind.node.BooleanNode.FALSE)) {
                return false;
            }
            Set<String> required = new HashSet<>();
            schema.path("required").forEach(field -> required.add(field.asText()));
            Set<String> properties = new HashSet<>();
            schema.path("properties").fieldNames().forEachRemaining(properties::add);
            if (!required.equals(properties)) {
                return false;
            }
        }
        for (String keyword : MAP_KEYWORDS) {
            for (JsonNode child : schema.path(keyword)) {
                if (!isCompatibleNode(child)) return false;
            }
        }
        for (String keyword : ARRAY_KEYWORDS) {
            for (JsonNode child : schema.path(keyword)) {
                if (!isCompatibleNode(child)) return false;
            }
        }
        for (String keyword : SCHEMA_KEYWORDS) {
            if (schema.path(keyword).isObject() && !isCompatibleNode(schema.path(keyword))) return false;
        }
        return true;
    }

    static ObjectNode normalizeStrict(ObjectNode schema) {
        if (!isStrictCompatible(schema)) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_STRICT_SCHEMA_REQUIRES_ALL_PROPERTIES_REQUIRED_AND_CLOSED_OBJECTS");
        }
        ObjectNode normalized = schema.deepCopy();
        closeObjects(normalized);
        return normalized;
    }

    private static void closeObjects(JsonNode schema) {
        if (!(schema instanceof ObjectNode object)) return;
        if (isObjectSchema(object)) {
            object.put("additionalProperties", false);
            if (!object.has("properties")) object.putObject("properties");
            if (!object.has("required")) object.putArray("required");
        }
        for (String keyword : MAP_KEYWORDS) object.path(keyword).forEach(ResponsesStructuredOutputSchema::closeObjects);
        for (String keyword : ARRAY_KEYWORDS) object.path(keyword).forEach(ResponsesStructuredOutputSchema::closeObjects);
        for (String keyword : SCHEMA_KEYWORDS) closeObjects(object.path(keyword));
    }

    private static boolean isObjectSchema(JsonNode schema) {
        if (schema.has("properties") || "object".equals(schema.path("type").asText())) return true;
        for (JsonNode type : schema.path("type")) {
            if ("object".equals(type.asText())) return true;
        }
        return false;
    }
}
