package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Translates caller linkage between Claude code-execution tool calls and
 * OpenAI Responses programmatic tool calls.
 */
final class ResponsesProgrammaticToolBridge {

    private static final String CLAUDE_DIRECT_CALLER = "direct";
    private static final String CLAUDE_CODE_EXECUTION_CALLER_PREFIX = "code_execution_";
    private static final String CLAUDE_CODE_EXECUTION_CALLER = "code_execution_20260120";
    private static final String OPENAI_DIRECT_CALLER = "direct";
    private static final String OPENAI_PROGRAMMATIC_CALLER = "programmatic";
    private static final String OPENAI_PROGRAM_CALLER_TYPE = "program";
    private static final String CLAUDE_PROGRAM_TOOL_ID_PREFIX = "srvtoolu_api2api_program_";

    private ResponsesProgrammaticToolBridge() {
    }

    static AllowedCallersMapping toResponsesAllowedCallers(ObjectMapper objectMapper, JsonNode allowedCallers) {
        if (allowedCallers == null || allowedCallers.isNull() || allowedCallers.isMissingNode()) {
            return AllowedCallersMapping.directOnly();
        }
        if (!allowedCallers.isArray()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_ALLOWED_CALLERS_MUST_BE_ARRAY");
        }
        if (allowedCallers.isEmpty()) {
            return AllowedCallersMapping.directOnly();
        }
        Set<String> mappedValues = new LinkedHashSet<>();
        for (JsonNode caller : allowedCallers) {
            String value = caller.asText("");
            if (CLAUDE_DIRECT_CALLER.equals(value)) {
                mappedValues.add(OPENAI_DIRECT_CALLER);
            } else if (value.startsWith(CLAUDE_CODE_EXECUTION_CALLER_PREFIX)) {
                mappedValues.add(OPENAI_PROGRAMMATIC_CALLER);
            } else {
                throw new ProtocolConversionException(
                        "CLAUDE_RESPONSES_UNSUPPORTED_ALLOWED_CALLER: " + value);
            }
        }
        ArrayNode mapped = objectMapper.createArrayNode();
        mappedValues.forEach(mapped::add);
        return new AllowedCallersMapping(mapped, mappedValues.contains(OPENAI_PROGRAMMATIC_CALLER));
    }

    static ObjectNode toResponsesCaller(ObjectMapper objectMapper, JsonNode claudeCaller) {
        if (claudeCaller == null || claudeCaller.isNull() || claudeCaller.isMissingNode()) {
            return null;
        }
        String type = claudeCaller.path("type").asText(CLAUDE_DIRECT_CALLER);
        if (CLAUDE_DIRECT_CALLER.equals(type)) {
            return null;
        }
        if (!type.startsWith(CLAUDE_CODE_EXECUTION_CALLER_PREFIX)) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_TOOL_CALLER: " + type);
        }
        String toolId = claudeCaller.path("tool_id").asText("");
        if (toolId.isBlank()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_PROGRAMMATIC_CALLER_TOOL_ID_REQUIRED");
        }
        ObjectNode caller = objectMapper.createObjectNode();
        caller.put("type", OPENAI_PROGRAM_CALLER_TYPE);
        caller.put("caller_id", decodeProgramCallerId(toolId));
        return caller;
    }

    static ObjectNode toClaudeCaller(ObjectMapper objectMapper, JsonNode responsesCaller) {
        if (responsesCaller == null || responsesCaller.isNull() || responsesCaller.isMissingNode()) {
            return null;
        }
        String type = responsesCaller.path("type").asText("");
        if (type.isBlank() || OPENAI_DIRECT_CALLER.equals(type)) {
            return null;
        }
        if (!OPENAI_PROGRAM_CALLER_TYPE.equals(type)) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_UNSUPPORTED_TOOL_CALLER: " + type);
        }
        String callerId = responsesCaller.path("caller_id").asText("");
        if (callerId.isBlank()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_PROGRAM_CALLER_ID_REQUIRED");
        }
        ObjectNode caller = objectMapper.createObjectNode();
        caller.put("type", CLAUDE_CODE_EXECUTION_CALLER);
        caller.put("tool_id", toClaudeProgramToolId(callerId));
        return caller;
    }

    static String toClaudeProgramToolId(String callerId) {
        if (callerId == null || callerId.isBlank()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_PROGRAM_CALLER_ID_REQUIRED");
        }
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(callerId.getBytes(StandardCharsets.UTF_8));
        return CLAUDE_PROGRAM_TOOL_ID_PREFIX + encoded;
    }

    static boolean isSyntheticProgramToolId(String toolId) {
        return toolId != null && toolId.startsWith(CLAUDE_PROGRAM_TOOL_ID_PREFIX);
    }

    private static String decodeProgramCallerId(String toolId) {
        if (!toolId.startsWith(CLAUDE_PROGRAM_TOOL_ID_PREFIX)) {
            throw new ProtocolConversionException(
                    "CLAUDE_RESPONSES_PROGRAMMATIC_CALLER_NOT_CREATED_BY_GATEWAY");
        }
        String encoded = toolId.substring(CLAUDE_PROGRAM_TOOL_ID_PREFIX.length());
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new ProtocolConversionException(
                    "CLAUDE_RESPONSES_INVALID_PROGRAMMATIC_CALLER_TOOL_ID", exception);
        }
    }

    record AllowedCallersMapping(ArrayNode values, boolean programmatic) {

        private static AllowedCallersMapping directOnly() {
            return new AllowedCallersMapping(null, false);
        }
    }
}
