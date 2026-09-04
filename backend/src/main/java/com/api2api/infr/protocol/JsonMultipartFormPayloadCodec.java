package com.api2api.infr.protocol;

import com.api2api.application.gateway.MultipartFormPayload;
import com.api2api.application.gateway.MultipartFormPayload.FilePart;
import com.api2api.application.gateway.MultipartFormPayload.TextField;
import com.api2api.application.gateway.MultipartFormPayloadCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * JSON envelope for multipart forms.
 *
 * <p>Text fields become top-level members named after the form field; a repeated name becomes an
 * array. Multipart carries text only, so the exact values {@code true}/{@code false} are written as
 * JSON booleans: the protocol contract reads {@code stream} as a boolean, and the text form is
 * restored verbatim on decode. Files live under a reserved member with their bytes base64-encoded.</p>
 */
@Component
public class JsonMultipartFormPayloadCodec implements MultipartFormPayloadCodec {

    static final String FILES_MEMBER = "_multipart_files";
    private static final String FILE_NAME = "name";
    private static final String FILE_FILENAME = "filename";
    private static final String FILE_CONTENT_TYPE = "content_type";
    private static final String FILE_DATA = "data";

    private final ObjectMapper objectMapper;

    public JsonMultipartFormPayloadCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    @Override
    public String encode(MultipartFormPayload payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        ObjectNode root = objectMapper.createObjectNode();
        groupByName(payload.fields()).forEach((name, values) -> {
            if (values.size() == 1) {
                root.set(name, scalar(values.get(0)));
                return;
            }
            ArrayNode array = root.putArray(name);
            values.forEach(value -> array.add(scalar(value)));
        });
        ArrayNode files = root.putArray(FILES_MEMBER);
        for (FilePart file : payload.files()) {
            ObjectNode node = files.addObject();
            node.put(FILE_NAME, file.name());
            node.put(FILE_FILENAME, file.filename());
            node.put(FILE_CONTENT_TYPE, file.contentType());
            node.put(FILE_DATA, Base64.getEncoder().encodeToString(file.content()));
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to encode multipart form envelope", exception);
        }
    }

    @Override
    public MultipartFormPayload decode(String envelope) {
        JsonNode root = readEnvelope(envelope);
        List<TextField> fields = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> members = root.fields();
        while (members.hasNext()) {
            Map.Entry<String, JsonNode> member = members.next();
            if (FILES_MEMBER.equals(member.getKey())) {
                continue;
            }
            JsonNode value = member.getValue();
            if (value.isArray()) {
                value.forEach(element -> fields.add(new TextField(member.getKey(), text(element))));
            } else {
                fields.add(new TextField(member.getKey(), text(value)));
            }
        }
        List<FilePart> files = new ArrayList<>();
        for (JsonNode node : root.path(FILES_MEMBER)) {
            files.add(new FilePart(
                    node.path(FILE_NAME).asText(),
                    textOrNull(node.get(FILE_FILENAME)),
                    textOrNull(node.get(FILE_CONTENT_TYPE)),
                    Base64.getDecoder().decode(node.path(FILE_DATA).asText(""))
            ));
        }
        return new MultipartFormPayload(fields, files);
    }

    private JsonNode readEnvelope(String envelope) {
        if (envelope == null || envelope.isBlank()) {
            throw new IllegalStateException("Multipart form envelope must not be blank");
        }
        try {
            JsonNode root = objectMapper.readTree(envelope);
            if (!root.isObject()) {
                throw new IllegalStateException("Multipart form envelope must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Multipart form envelope is not valid JSON", exception);
        }
    }

    private static Map<String, List<String>> groupByName(List<TextField> fields) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (TextField field : fields) {
            grouped.computeIfAbsent(field.name(), ignored -> new ArrayList<>()).add(field.value());
        }
        return grouped;
    }

    private JsonNode scalar(String value) {
        if ("true".equals(value) || "false".equals(value)) {
            return objectMapper.getNodeFactory().booleanNode(Boolean.parseBoolean(value));
        }
        return objectMapper.getNodeFactory().textNode(value);
    }

    private static String text(JsonNode node) {
        if (node.isNull() || node.isMissingNode()) {
            return "";
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
