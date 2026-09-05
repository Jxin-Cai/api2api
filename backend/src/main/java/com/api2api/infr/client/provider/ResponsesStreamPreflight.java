package com.api2api.infr.client.provider;

import com.api2api.application.gateway.UpstreamResponseMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Looks through a bounded Responses SSE prelude before the gateway commits its downstream stream.
 * Only an explicit overload before output is retryable. Everything else, including unknown events,
 * malformed payloads and errors after output starts, is replayed byte-for-byte without inspection.
 */
final class ResponsesStreamPreflight {

    static final int MAX_PREFIX_BYTES = 64 * 1024;
    private static final int MAX_PRELUDE_EVENTS = 32;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> OVERLOAD_CODES = Set.of(
            "overloaded_error", "server_overloaded", "server_is_overloaded");
    private static final Set<String> GENERIC_SERVER_CODES = Set.of("", "server_error", "api_error");

    private ResponsesStreamPreflight() {}

    static InputStream inspect(InputStream body, Instant startedAt, UpstreamResponseMetadata metadata)
            throws IOException {
        InputStream buffered = new BufferedInputStream(body);
        ByteArrayOutputStream prefix = new ByteArrayOutputStream();
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        StringBuilder data = new StringBuilder();
        String event = "";
        int frames = 0;
        boolean previousCarriageReturn = false;
        while (prefix.size() < MAX_PREFIX_BYTES && frames < MAX_PRELUDE_EVENTS) {
            int value = buffered.read();
            if (value < 0) {
                break;
            }
            prefix.write(value);
            if (value == '\n' && previousCarriageReturn) {
                previousCarriageReturn = false;
                continue;
            }
            previousCarriageReturn = value == '\r';
            if (value != '\n' && value != '\r') {
                line.write(value);
                continue;
            }
            String text = line.toString(StandardCharsets.UTF_8);
            line.reset();
            if (text.isEmpty()) {
                frames++;
                Decision decision = classify(event, data.toString());
                if (decision == Decision.OVERLOADED) {
                    throw new UpstreamStreamOverloadedException(
                            Duration.between(startedAt, Instant.now()).toMillis(), metadata);
                }
                if (decision == Decision.PASSTHROUGH) {
                    break;
                }
                event = "";
                data.setLength(0);
            } else if (text.startsWith("event:")) {
                event = fieldValue(text);
            } else if (text.startsWith("data:")) {
                data.append(fieldValue(text)).append('\n');
            } else if (text.equals("data")) {
                data.append('\n');
            }
        }
        return new SequenceInputStream(new ByteArrayInputStream(prefix.toByteArray()), buffered);
    }

    private static String fieldValue(String line) {
        String value = line.substring(line.indexOf(':') + 1);
        return value.startsWith(" ") ? value.substring(1) : value;
    }

    private static Decision classify(String event, String data) {
        if (data.isEmpty()) {
            return event.isEmpty() ? Decision.PRELUDE : Decision.PASSTHROUGH;
        }
        JsonNode payload;
        try {
            payload = JSON.readTree(data);
        } catch (JsonProcessingException malformedJson) {
            return Decision.PASSTHROUGH;
        }
        if (payload == null || !payload.isObject()) {
            return Decision.PASSTHROUGH;
        }
        String type = payload.path("type").asText(event);
        if (!event.isEmpty() && !event.equals(type)) {
            return Decision.PASSTHROUGH;
        }
        JsonNode response = payload.path("response");
        return switch (type) {
            case "response.created", "response.in_progress" -> Decision.PRELUDE;
            case "response.failed" -> hasNoOutput(response) && isOverloaded(response.path("error"))
                    ? Decision.OVERLOADED : Decision.PASSTHROUGH;
            case "error", "response.error" -> isOverloaded(payload.has("error") ? payload.path("error") : payload)
                    ? Decision.OVERLOADED : Decision.PASSTHROUGH;
            default -> Decision.PASSTHROUGH;
        };
    }

    private static boolean hasNoOutput(JsonNode response) {
        JsonNode output = response.path("output");
        return output.isMissingNode() || output.isNull() || (output.isArray() && output.isEmpty());
    }

    private static boolean isOverloaded(JsonNode error) {
        String code = error.path("code").asText("").toLowerCase(Locale.ROOT);
        String type = error.path("type").asText("").toLowerCase(Locale.ROOT);
        if (OVERLOAD_CODES.contains(code) || OVERLOAD_CODES.contains(type)) {
            return true;
        }
        // Never infer retryability from arbitrary payload text or from auth/validation errors.
        return GENERIC_SERVER_CODES.contains(code)
                && (GENERIC_SERVER_CODES.contains(type) || type.equals("error"))
                && error.path("message").asText("").toLowerCase(Locale.ROOT).contains("overloaded");
    }

    private enum Decision { PRELUDE, PASSTHROUGH, OVERLOADED }
}
