package com.api2api.ohs.http.gateway;

import com.api2api.domain.channel.model.ProtocolType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Stateless Responses image-tool adaptation. Merely advertising a tool must not execute it. */
final class ResponsesImageRequest {
    static final String IMAGE_TOOL = "image_generation";
    private final ObjectNode body;
    private final ObjectNode tool;
    private final String functionName;

    private ResponsesImageRequest(ObjectNode body, ObjectNode tool) {
        this.body = body;
        this.tool = tool;
        String candidate = "api2api_generate_image";
        while (hasFunction(body.path("tools"), candidate)) candidate += "_";
        this.functionName = candidate;
    }

    static ResponsesImageRequest parse(ObjectMapper mapper, String rawBody, String defaultModel) {
        ObjectNode body = readObject(mapper, rawBody);
        List<ObjectNode> tools = new ArrayList<>();
        for (JsonNode tool : body.path("tools")) {
            if (IMAGE_TOOL.equals(tool.path("type").asText())) tools.add((ObjectNode) tool.deepCopy());
        }
        if (tools.isEmpty()) return null;
        if (tools.size() != 1) throw invalid("Only one image_generation tool definition is supported");
        if (!body.path("model").isTextual() || body.path("model").asText().isBlank()) {
            throw invalid("model is required");
        }
        for (String field : List.of("previous_response_id", "conversation")) {
            if (body.hasNonNull(field)) throw invalid(field + " is unavailable for the stateless image bridge; resend input history");
        }
        if (body.path("background").asBoolean() || body.path("store").asBoolean()) {
            throw invalid("The image bridge requires background=false and store=false");
        }
        ObjectNode tool = tools.get(0);
        if (!tool.hasNonNull("model")) tool.put("model", defaultModel);
        if (!tool.path("model").isTextual() || tool.path("model").asText().isBlank()) {
            throw invalid("image_generation.model must be a non-empty model name");
        }
        ImageAction.parse(tool.path("action").asText("auto"));
        if (tool.has("partial_images") && (!tool.path("partial_images").canConvertToInt()
                || !tool.path("partial_images").isIntegralNumber()
                || tool.path("partial_images").asInt() < 0 || tool.path("partial_images").asInt() > 3)) {
            throw invalid("partial_images must be an integer between 0 and 3");
        }
        if (tool.path("partial_images").asInt() > 0 && !body.path("stream").asBoolean()) {
            throw invalid("partial_images requires stream=true");
        }
        if (body.hasNonNull("max_tool_calls") && (!body.path("max_tool_calls").isIntegralNumber()
                || !body.path("max_tool_calls").canConvertToInt() || body.path("max_tool_calls").asInt() < 1)) {
            throw invalid("max_tool_calls must be a positive integer");
        }
        return new ResponsesImageRequest(body, tool);
    }

    boolean streaming() { return body.path("stream").asBoolean(false); }
    String model() { return body.path("model").asText(); }
    String imageModel() { return tool.path("model").asText(); }
    ObjectNode body() { return body.deepCopy(); }
    ObjectNode tool() { return tool.deepCopy(); }

    boolean forced() {
        JsonNode choice = body.path("tool_choice");
        return IMAGE_TOOL.equals(choice.path("type").asText())
                || ("required".equals(choice.asText()) && body.path("tools").size() == 1)
                || ("allowed_tools".equals(choice.path("type").asText())
                    && "required".equals(choice.path("mode").asText()) && choice.path("tools").size() == 1
                    && IMAGE_TOOL.equals(choice.path("tools").get(0).path("type").asText()));
    }

    boolean imageAllowed() {
        JsonNode choice = body.path("tool_choice");
        if (choice.isMissingNode() || choice.isNull()) return true;
        if (choice.isTextual()) return List.of("auto", "required").contains(choice.asText());
        if (IMAGE_TOOL.equals(choice.path("type").asText())) return true;
        if ("allowed_tools".equals(choice.path("type").asText())) {
            for (JsonNode selected : choice.path("tools")) {
                if (IMAGE_TOOL.equals(selected.path("type").asText())) return true;
            }
        }
        return false;
    }

    int maxImageCalls() { return Math.min(4, body.path("max_tool_calls").asInt(4)); }

    boolean isImageCall(JsonNode item) {
        return "function_call".equals(item.path("type").asText())
                && functionName.equals(item.path("name").asText());
    }

    ObjectNode plannerRequest(ObjectMapper mapper) {
        ObjectNode planned = body.deepCopy();
        planned.put("stream", false);
        planned.put("store", false);
        planned.remove("stream_options");
        ArrayNode tools = planned.putArray("tools");
        boolean disabled = !imageAllowed();
        for (JsonNode original : body.path("tools")) {
            if (!IMAGE_TOOL.equals(original.path("type").asText())) tools.add(original.deepCopy());
            else if (!disabled) tools.add(functionTool(mapper));
        }
        if (tools.isEmpty()) {
            planned.remove("tools");
            planned.remove("tool_choice");
        }
        if ("allowed_tools".equals(planned.path("tool_choice").path("type").asText())) {
            for (JsonNode selected : planned.path("tool_choice").path("tools")) {
                if (IMAGE_TOOL.equals(selected.path("type").asText())) {
                    ((ObjectNode) selected).put("type", "function").put("name", functionName);
                }
            }
        }
        // A generated image is portable content, not a provider-owned function call ID.
        if (planned.path("input").isArray()) {
            ArrayNode input = planned.putArray("input");
            for (JsonNode item : body.path("input")) {
                if (!"image_generation_call".equals(item.path("type").asText())) {
                    input.add(item.deepCopy());
                } else {
                    if (item.path("result").asText().isBlank()) throw invalid("Resend the result of prior image_generation_call items");
                    ObjectNode message = input.addObject().put("role", "user");
                    ArrayNode content = message.putArray("content");
                    content.addObject().put("type", "input_text").put("text", "Previously generated image:");
                    content.addObject().put("type", "input_image").put("image_url", resultDataUrl(item));
                }
            }
        }
        return planned;
    }

    private ObjectNode functionTool(ObjectMapper mapper) {
        ObjectNode function = mapper.createObjectNode().put("type", "function").put("name", functionName)
                .put("description", "Generate or edit an image only when the user requests it. Supply a self-contained image prompt. "
                        + "Use action=edit to modify images in the conversation, or generate for a new image.")
                .put("strict", true);
        ObjectNode schema = function.putObject("parameters").put("type", "object").put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("prompt").put("type", "string");
        properties.putObject("action").put("type", "string").putArray("enum").add("generate").add("edit");
        schema.putArray("required").add("prompt").add("action");
        return function;
    }

    String prompt() {
        List<String> parts = new ArrayList<>();
        if (!body.path("instructions").asText().isBlank()) parts.add(body.path("instructions").asText());
        JsonNode input = body.path("input");
        if (input.isTextual()) parts.add(input.asText());
        else for (JsonNode message : input) {
            if (message.path("content").isTextual()) parts.add(message.path("content").asText());
            else for (JsonNode content : message.path("content")) {
                if (List.of("input_text", "output_text").contains(content.path("type").asText())) {
                    parts.add(content.path("text").asText());
                }
            }
        }
        String prompt = String.join("\n\n", parts).strip();
        if (prompt.isBlank()) throw invalid("Image generation requires a text prompt in input");
        return prompt;
    }

    List<String> inputImages() {
        List<String> images = new ArrayList<>();
        for (JsonNode item : body.path("input")) {
            if ("image_generation_call".equals(item.path("type").asText())) {
                if (item.path("result").asText().isBlank()) throw invalid("Resend the result of prior image_generation_call items");
                images.add(resultDataUrl(item));
            }
            for (JsonNode content : item.path("content")) {
                if ("input_image".equals(content.path("type").asText())) {
                    if (content.hasNonNull("file_id")) throw invalid("Image edits require inline image_url data, not file_id");
                    images.add(content.path("image_url").asText());
                }
            }
        }
        return List.copyOf(images);
    }

    static String resultDataUrl(JsonNode item) {
        String format = item.path("output_format").asText("png");
        return "data:image/" + format + ";base64," + item.path("result").asText();
    }

    static ObjectNode readObject(ObjectMapper mapper, String json) {
        try {
            JsonNode parsed = mapper.readTree(json);
            if (parsed instanceof ObjectNode object) return object;
            throw invalid("Expected a JSON object");
        } catch (JsonProcessingException exception) {
            throw invalid("Invalid JSON request");
        }
    }

    static GatewayProtocolException invalid(String message) {
        return GatewayProtocolException.badRequest(ProtocolType.OPENAI_RESPONSES, message);
    }

    private static boolean hasFunction(JsonNode tools, String name) {
        for (JsonNode tool : tools) {
            if (name.equals(tool.path("name").asText())) return true;
            if (hasFunction(tool.path("tools"), name)) return true;
        }
        return false;
    }

    enum ImageAction {
        AUTO, GENERATE, EDIT;
        static ImageAction parse(String value) {
            try { return valueOf(value.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException exception) { throw invalid("image action must be auto, generate or edit"); }
        }
    }
}
