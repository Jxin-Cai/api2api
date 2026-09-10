package com.api2api.ohs.http.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/** Consumes Images SSE while the shared passthrough service extracts its original usage. */
final class ResponsesImageSseAdapter extends OutputStream {
    private static final int MAX_EVENT_BYTES = 100 * 1024 * 1024;
    private final ObjectMapper mapper;
    private final ResponsesImageResponseWriter writer;
    private final ResponsesImagePayloadMapper.ImageRequest request;
    private final int index;
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();
    private final StringBuilder data = new StringBuilder();
    private String eventName = "";
    private JsonNode completed;

    ResponsesImageSseAdapter(ObjectMapper mapper, ResponsesImageResponseWriter writer,
            ResponsesImagePayloadMapper.ImageRequest request, int index) {
        this.mapper = mapper;
        this.writer = writer;
        this.request = request;
        this.index = index;
    }

    @Override
    public void write(int value) throws IOException {
        if (value == '\n') consumeLine();
        else {
            if (line.size() >= MAX_EVENT_BYTES) throw ResponsesImageBridgeFailure.invalidUpstream("Image SSE event exceeds size limit");
            line.write(value);
        }
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        int start = offset;
        for (int i = offset; i < offset + length; i++) {
            if (bytes[i] == '\n') {
                append(bytes, start, i - start);
                consumeLine();
                start = i + 1;
            }
        }
        append(bytes, start, offset + length - start);
    }

    private void append(byte[] bytes, int offset, int length) {
        if ((long) line.size() + length > MAX_EVENT_BYTES) throw ResponsesImageBridgeFailure.invalidUpstream("Image SSE event exceeds size limit");
        line.write(bytes, offset, length);
    }

    private void consumeLine() throws IOException {
        String value = line.toString(StandardCharsets.UTF_8);
        line.reset();
        if (value.endsWith("\r")) value = value.substring(0, value.length() - 1);
        if (value.isEmpty()) dispatch();
        else if (value.startsWith("event:")) eventName = value.substring(6).strip();
        else if (value.startsWith("data:")) {
            if ((long) data.length() + value.length() > MAX_EVENT_BYTES) {
                throw ResponsesImageBridgeFailure.invalidUpstream("Image SSE event exceeds size limit");
            }
            if (!data.isEmpty()) data.append('\n');
            data.append(value.substring(5).stripLeading());
        }
    }

    private void dispatch() throws IOException {
        if (data.isEmpty()) { eventName = ""; return; }
        String payload = data.toString();
        data.setLength(0);
        String namedType = eventName;
        eventName = "";
        if ("[DONE]".equals(payload)) return;
        JsonNode event;
        try { event = mapper.readTree(payload); }
        catch (JsonProcessingException exception) { throw ResponsesImageBridgeFailure.invalidUpstream("Invalid Images SSE JSON"); }
        if (event == null || !event.isObject()) throw ResponsesImageBridgeFailure.invalidUpstream("Invalid Images SSE event");
        String type = event.path("type").asText(namedType);
        if ("error".equals(type) || event.hasNonNull("error")) {
            JsonNode error = event.hasNonNull("error") ? event.get("error") : event;
            var body = mapper.createObjectNode().set("error", error);
            throw new ResponsesImageBridgeFailure(GatewayRawResponse.of(body.toString(), 502, MediaType.APPLICATION_JSON));
        }
        if (type.endsWith(".partial_image")) writer.partialImage(index, event);
        else if ("image_generation.completed".equals(type) || "image_edit.completed".equals(type)) {
            if (completed != null) throw ResponsesImageBridgeFailure.invalidUpstream("Duplicate Images completion event");
            completed = event;
        }
    }

    void finish() throws IOException {
        if (line.size() > 0) consumeLine();
        dispatch();
        if (completed == null) throw ResponsesImageBridgeFailure.invalidUpstream("Images stream ended before completion");
        if (completed.path("b64_json").asText().isBlank()) throw ResponsesImageBridgeFailure.invalidUpstream("Images upstream returned no base64 image");
        for (String field : java.util.List.of("input_tokens", "output_tokens")) {
            JsonNode tokens = completed.path("usage").path(field);
            if (!tokens.isMissingNode() && (!tokens.isIntegralNumber() || !tokens.canConvertToLong() || tokens.asLong() < 0)) {
                throw ResponsesImageBridgeFailure.invalidUpstream("Invalid Images usage token count");
            }
        }
    }

    void publish() throws IOException {
        writer.finishImage(index, completed, request);
        writer.addUsage(completed.path("usage"), true);
    }

    JsonNode completed() { return completed; }
}
