package com.api2api.infr.protocol.contract;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import com.api2api.domain.protocolcontract.model.ProtocolContractViolationException;
import com.api2api.domain.protocolmetadata.model.FieldSection;
import com.api2api.domain.protocolmetadata.model.UsageDirection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Complete executable contract for one external protocol. */
public final class ProtocolContract {

    private static final Set<String> TOOL_CONTENT_TYPES = Set.of(
            "tool_use", "tool_result", "server_tool_use", "mcp_tool_use", "mcp_tool_result",
            "web_search_tool_result", "web_fetch_tool_result", "code_execution_tool_result",
            "bash_code_execution_tool_result", "text_editor_code_execution_tool_result", "tool_search_tool_result"
    );
    private static final Set<String> REASONING_CONTENT_TYPES = Set.of(
            "thinking", "redacted_thinking", "reasoning"
    );

    private final ProtocolType protocolType;
    private final String displayName;
    private final String apiSpecVersion;
    private final String description;
    private final String defaultEndpointPath;
    private final ObjectMapper objectMapper;
    private final List<ProtocolFieldRef> fields;
    private final ProtocolShape requestShape;
    private final ProtocolShape responseShape;
    private final ProtocolShape streamEventShape;

    public ProtocolContract(
            ProtocolType protocolType,
            String displayName,
            String apiSpecVersion,
            String description,
            String defaultEndpointPath,
            ObjectMapper objectMapper,
            List<ProtocolFieldRef> fields
    ) {
        this.protocolType = Objects.requireNonNull(protocolType, "protocolType must not be null");
        this.displayName = requireText(displayName, "displayName");
        this.apiSpecVersion = requireText(apiSpecVersion, "apiSpecVersion");
        this.description = description;
        this.defaultEndpointPath = requireText(defaultEndpointPath, "defaultEndpointPath");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.fields = List.copyOf(fields);
        this.requestShape = new ProtocolShape(ProtocolShapeKind.REQUEST,
                fields.stream().filter(field -> field.direction() != UsageDirection.OUTPUT).toList(), true);
        this.responseShape = new ProtocolShape(ProtocolShapeKind.RESPONSE,
                fields.stream().filter(field -> field.direction() != UsageDirection.INPUT)
                        .filter(field -> field.section() != FieldSection.STREAMING).toList(), true);
        this.streamEventShape = new ProtocolShape(ProtocolShapeKind.STREAM_EVENT,
                fields.stream().filter(field -> field.direction() != UsageDirection.INPUT)
                        .filter(field -> field.section() == FieldSection.STREAMING
                                || field.section() == FieldSection.USAGE).toList(), false);
        verifyEveryMetadataFieldIsExecutable();
    }

    public ParsedGatewayRequest parseRequest(String rawBody) {
        JsonNode root = parseRequestNode(rawBody);
        ProtocolFieldRef model = requestShape.requireField("model");
        boolean streaming = optionalField("stream").readBoolean(root, ProtocolShapeKind.REQUEST, false);
        boolean toolCalling = present(root, "tools") || present(root, "tool_choice")
                || present(root, "toolConfig") || present(root, "mcp_servers")
                || containsContentType(root, TOOL_CONTENT_TYPES);
        boolean reasoning = present(root, "thinking") || present(root, "reasoning")
                || present(root, "additionalModelRequestFields.thinking")
                || present(root, "output_config.effort")
                || present(root, "reasoning_effort")
                || containsContentType(root, REASONING_CONTENT_TYPES);
        return new ParsedGatewayRequest(rawBody, model.readText(root, ProtocolShapeKind.REQUEST),
                streaming, toolCalling, reasoning);
    }

    public JsonNode parseRequestNode(String rawBody) {
        return requestShape.parse(readObject(rawBody, ProtocolShapeKind.REQUEST));
    }

    public JsonNode parseResponse(String rawBody) {
        return responseShape.parse(readObject(rawBody, ProtocolShapeKind.RESPONSE));
    }

    public JsonNode parseStreamEvent(String rawBody) {
        return streamEventShape.parse(readObject(rawBody, ProtocolShapeKind.STREAM_EVENT));
    }

    public String buildResponse(String rawBody) {
        parseResponse(rawBody);
        return rawBody;
    }

    private JsonNode readObject(String rawBody, ProtocolShapeKind kind) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new ProtocolContractViolationException(kind + " body must not be blank");
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (JsonProcessingException exception) {
            throw new ProtocolContractViolationException(kind + " body must be valid JSON", exception);
        }
    }

    private boolean present(JsonNode root, String path) {
        return optionalField(path).presentAndNotEmpty(root, ProtocolShapeKind.REQUEST);
    }

    private ProtocolFieldRef optionalField(String path) {
        return requestShape.fields().stream().filter(field -> field.path().equals(path)).findFirst()
                .orElseGet(() -> ProtocolFieldRef.runtime(path, inferRuntimeType(path)));
    }

    private com.api2api.domain.protocolmetadata.model.FieldType inferRuntimeType(String path) {
        return path.equals("stream")
                ? com.api2api.domain.protocolmetadata.model.FieldType.BOOLEAN
                : com.api2api.domain.protocolmetadata.model.FieldType.OBJECT;
    }

    private boolean containsContentType(JsonNode root, Set<String> expectedTypes) {
        ProtocolFieldRef contentTypes = ProtocolFieldRef.runtime(
                protocolType == ProtocolType.OPENAI_RESPONSES
                        ? "input[].content[].type"
                        : "messages[].content[].type",
                com.api2api.domain.protocolmetadata.model.FieldType.STRING
        );
        return contentTypes.readAll(root, ProtocolShapeKind.REQUEST).stream()
                .filter(JsonNode::isTextual).map(JsonNode::asText).anyMatch(expectedTypes::contains);
    }

    private void verifyEveryMetadataFieldIsExecutable() {
        Set<ProtocolFieldRef> executable = new LinkedHashSet<>();
        executable.addAll(requestShape.fields());
        executable.addAll(responseShape.fields());
        executable.addAll(streamEventShape.fields());
        if (!executable.containsAll(fields)) {
            throw new IllegalArgumentException("Every metadata field must belong to an executable shape");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public ProtocolType protocolType() { return protocolType; }
    public String displayName() { return displayName; }
    public String apiSpecVersion() { return apiSpecVersion; }
    public String description() { return description; }
    public String defaultEndpointPath() { return defaultEndpointPath; }
    public List<ProtocolFieldRef> fields() { return fields; }
    public ProtocolShape requestShape() { return requestShape; }
    public ProtocolShape responseShape() { return responseShape; }
    public ProtocolShape streamEventShape() { return streamEventShape; }
}
