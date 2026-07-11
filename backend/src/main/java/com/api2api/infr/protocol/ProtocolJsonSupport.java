package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/**
 * 协议转换 JSON 读写辅助，异常信息只包含字段路径而不包含正文。
 */
final class ProtocolJsonSupport {
    private final ObjectMapper objectMapper;

    ProtocolJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    JsonNode parse(String body, String direction) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ProtocolConversionException("failed to parse " + direction + " JSON body", e);
        }
    }

    String stringify(JsonNode node, String direction) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new ProtocolConversionException("failed to write " + direction + " JSON body", e);
        }
    }

    ObjectNode objectNode() {
        return objectMapper.createObjectNode();
    }

    ArrayNode arrayNode() {
        return objectMapper.createArrayNode();
    }

    JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    ObjectMapper objectMapper() {
        return objectMapper;
    }

    JsonNode path(JsonNode node, String pointer, String direction, boolean required) {
        JsonNode current = node.at(pointer);
        if ((current.isMissingNode() || current.isNull()) && required) {
            throw new ProtocolConversionException("missing required field for " + direction + ": " + pointer);
        }
        return current;
    }

    String text(JsonNode node, String pointer, String direction, boolean required) {
        JsonNode current = path(node, pointer, direction, required);
        if (current.isMissingNode() || current.isNull()) {
            return null;
        }
        if (!current.isTextual()) {
            throw new ProtocolConversionException("field must be text for " + direction + ": " + pointer);
        }
        return current.asText();
    }

    long longValue(JsonNode node, String pointer, String direction, long defaultValue) {
        JsonNode current = path(node, pointer, direction, false);
        if (current.isMissingNode() || current.isNull()) {
            return defaultValue;
        }
        if (!current.canConvertToLong()) {
            throw new ProtocolConversionException("field must be integer for " + direction + ": " + pointer);
        }
        return current.asLong();
    }
}
