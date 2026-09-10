package com.api2api.ohs.http.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Per-request response state. The same item IDs and output indexes are used in JSON and SSE. */
final class ResponsesImageResponseWriter {
    private final ObjectMapper mapper;
    private final OutputStream stream;
    private final ObjectNode response;
    private int sequence;

    ResponsesImageResponseWriter(ObjectMapper mapper, ResponsesImageRequest request, OutputStream stream) {
        this.mapper = mapper;
        this.stream = stream;
        response = mapper.createObjectNode().put("id", "resp_" + UUID.randomUUID().toString().replace("-", ""))
                .put("object", "response").put("created_at", Instant.now().getEpochSecond())
                .put("status", "in_progress").put("model", request.model()).put("store", false);
        response.putNull("error");
        response.putNull("incomplete_details");
        response.putArray("output");
        response.putNull("usage");
        response.set("tools", request.body().path("tools"));
        response.set("tool_choice", request.body().path("tool_choice").isMissingNode()
                ? mapper.getNodeFactory().textNode("auto") : request.body().path("tool_choice"));
        response.put("parallel_tool_calls", request.body().path("parallel_tool_calls").asBoolean(true));
    }

    void start() throws IOException {
        event("response.created", mapper.createObjectNode().set("response", response));
        event("response.in_progress", mapper.createObjectNode().set("response", response));
    }

    int beginImage() throws IOException {
        ObjectNode image = mapper.createObjectNode().put("id", "ig_" + UUID.randomUUID().toString().replace("-", ""))
                .put("type", "image_generation_call").put("status", "in_progress");
        int index = output().size();
        output().add(image);
        itemEvent("response.output_item.added", index, image);
        imageEvent("in_progress", index, mapper.createObjectNode());
        imageEvent("generating", index, mapper.createObjectNode());
        return index;
    }

    void partialImage(int index, JsonNode data) throws IOException {
        String partial = data.path("b64_json").asText();
        if (partial.isBlank()) throw ResponsesImageBridgeFailure.invalidUpstream("Image partial event is missing b64_json");
        ObjectNode fields = mapper.createObjectNode().put("partial_image_b64", partial)
                .put("partial_image_index", data.path("partial_image_index").asInt());
        for (String name : List.of("size", "quality", "output_format", "background")) {
            if (data.hasNonNull(name)) fields.set(name, data.get(name));
        }
        imageEvent("partial_image", index, fields);
    }

    void finishImage(int index, JsonNode data, ResponsesImagePayloadMapper.ImageRequest request) throws IOException {
        String result = data.path("b64_json").asText();
        if (result.isBlank()) throw ResponsesImageBridgeFailure.invalidUpstream("Images upstream returned no base64 image");
        ObjectNode image = (ObjectNode) output().get(index);
        image.put("status", "completed").put("result", result).put("action", request.action());
        for (String name : List.of("size", "quality", "output_format", "background")) {
            JsonNode value = data.hasNonNull(name) ? data.get(name) : request.options().get(name);
            if (value != null) image.set(name, value);
        }
        if (data.hasNonNull("revised_prompt")) image.set("revised_prompt", data.get("revised_prompt"));
        if (!image.hasNonNull("output_format")) image.put("output_format", "png");
        imageEvent("completed", index, mapper.createObjectNode());
        itemEvent("response.output_item.done", index, image);
    }

    void appendItem(JsonNode item) throws IOException {
        if (!(item instanceof ObjectNode original)) throw ResponsesImageBridgeFailure.invalidUpstream("Invalid Responses output item");
        ObjectNode complete = original.deepCopy();
        if (!complete.hasNonNull("id")) complete.put("id", "item_" + UUID.randomUUID().toString().replace("-", ""));
        int index = output().size();
        ObjectNode added = complete.deepCopy();
        if (added.has("status")) added.put("status", "in_progress");
        String type = complete.path("type").asText();
        if ("message".equals(type)) added.putArray("content");
        if ("function_call".equals(type)) added.put("arguments", "");
        output().add(complete);
        itemEvent("response.output_item.added", index, added);
        if ("message".equals(type)) messageEvents(index, complete);
        if ("function_call".equals(type)) {
            ObjectNode fields = itemFields(index).put("delta", complete.path("arguments").asText());
            event("response.function_call_arguments.delta", fields);
            event("response.function_call_arguments.done", itemFields(index).put("arguments", complete.path("arguments").asText()));
        }
        itemEvent("response.output_item.done", index, complete);
    }

    private void messageEvents(int index, ObjectNode message) throws IOException {
        int contentIndex = 0;
        for (JsonNode part : message.path("content")) {
            ObjectNode fields = itemFields(index).put("content_index", contentIndex++);
            ObjectNode initial = part.deepCopy();
            if (initial.has("text")) initial.put("text", "");
            if (initial.has("refusal")) initial.put("refusal", "");
            event("response.content_part.added", fields.deepCopy().set("part", initial));
            String type = part.path("type").asText();
            if ("output_text".equals(type)) {
                event("response.output_text.delta", fields.deepCopy().put("delta", part.path("text").asText()));
                event("response.output_text.done", fields.deepCopy().put("text", part.path("text").asText()));
            } else if ("refusal".equals(type)) {
                event("response.refusal.delta", fields.deepCopy().put("delta", part.path("refusal").asText()));
                event("response.refusal.done", fields.deepCopy().put("refusal", part.path("refusal").asText()));
            }
            event("response.content_part.done", fields.deepCopy().set("part", part));
        }
    }

    void addUsage(JsonNode usage, boolean imageUsage) {
        if (!usage.isObject()) return;
        if (!response.path("usage").isObject()) response.putObject("usage");
        sumUsage((ObjectNode) response.get("usage"), usage);
        ObjectNode total = (ObjectNode) response.get("usage");
        total.put("total_tokens", total.path("input_tokens").asLong() + total.path("output_tokens").asLong());
        if (imageUsage) {
            ObjectNode tools = response.withObject("/tool_usage");
            sumUsage(tools.withObject("/image_generation"), usage);
        }
    }

    private void sumUsage(ObjectNode target, JsonNode source) {
        source.fields().forEachRemaining(field -> {
            if (field.getValue().isIntegralNumber()) {
                target.put(field.getKey(), target.path(field.getKey()).asLong() + field.getValue().asLong());
            } else if (field.getValue().isObject()) {
                sumUsage(target.withObject("/" + field.getKey()), field.getValue());
            }
        });
    }

    void finish(JsonNode plannedResponse) throws IOException {
        String status = plannedResponse == null ? "completed" : plannedResponse.path("status").asText("completed");
        if (!List.of("completed", "incomplete", "failed").contains(status)) {
            throw ResponsesImageBridgeFailure.invalidUpstream("Responses planner returned a non-terminal status");
        }
        response.put("status", status);
        if ("completed".equals(status)) response.put("completed_at", Instant.now().getEpochSecond());
        if (plannedResponse != null) {
            for (String field : List.of("error", "incomplete_details")) {
                if (plannedResponse.hasNonNull(field)) response.set(field, plannedResponse.get(field));
            }
        }
        event("response." + status, mapper.createObjectNode().set("response", response));
    }

    void fail(ObjectNode error) throws IOException {
        response.put("status", "failed");
        response.set("error", error);
        for (JsonNode item : output()) {
            if ("in_progress".equals(item.path("status").asText())) ((ObjectNode) item).put("status", "failed");
        }
        event("response.failed", mapper.createObjectNode().set("response", response));
    }

    ObjectNode response() { return response.deepCopy(); }
    private ArrayNode output() { return (ArrayNode) response.get("output"); }

    private ObjectNode itemFields(int index) {
        return mapper.createObjectNode().put("output_index", index).put("item_id", output().get(index).path("id").asText());
    }

    private void imageEvent(String phase, int index, ObjectNode fields) throws IOException {
        fields.setAll(itemFields(index));
        event("response.image_generation_call." + phase, fields);
    }

    private void itemEvent(String type, int index, JsonNode item) throws IOException {
        event(type, mapper.createObjectNode().put("output_index", index).set("item", item));
    }

    private void event(String type, ObjectNode fields) throws IOException {
        if (stream == null) return;
        ObjectNode event = fields.deepCopy().put("type", type).put("sequence_number", sequence++);
        stream.write(("event: " + type + "\ndata: " + event + "\n\n").getBytes(StandardCharsets.UTF_8));
        stream.flush();
    }
}
