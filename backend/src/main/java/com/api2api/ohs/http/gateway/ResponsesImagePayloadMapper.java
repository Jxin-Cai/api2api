package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.MultipartFormPayload;
import com.api2api.application.gateway.MultipartFormPayload.FilePart;
import com.api2api.application.gateway.MultipartFormPayload.TextField;
import com.api2api.application.gateway.MultipartFormPayloadCodec;
import com.api2api.application.gateway.ProtocolOperation;
import com.api2api.ohs.http.gateway.ResponsesImageRequest.ImageAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Converts image tool arguments into the existing Images JSON / multipart gateway contract. */
@Component
@RequiredArgsConstructor
public class ResponsesImagePayloadMapper {
    private static final int MAX_IMAGE_BYTES = 50 * 1024 * 1024;
    private static final int MAX_REQUEST_BYTES = 100 * 1024 * 1024;
    private static final int MAX_IMAGES = 16;
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final List<String> OPTIONS = List.of("size", "quality", "background", "output_format",
            "output_compression", "moderation", "input_fidelity");
    private final ObjectMapper mapper;
    private final MultipartFormPayloadCodec multipartCodec;

    ImageRequest map(ResponsesImageRequest request, String prompt, String selectedAction) {
        if (prompt == null || prompt.isBlank()) throw ResponsesImageRequest.invalid("Image prompt must not be empty");
        ObjectNode tool = request.tool();
        ImageAction action = ImageAction.parse(tool.path("action").asText("auto"));
        List<String> inputImages = request.inputImages();
        if (action == ImageAction.AUTO) {
            action = selectedAction == null ? (inputImages.isEmpty() ? ImageAction.GENERATE : ImageAction.EDIT)
                    : ImageAction.parse(selectedAction);
        }
        if (action == ImageAction.AUTO) throw ResponsesImageRequest.invalid("The selected image action must be generate or edit");
        ObjectNode payload = mapper.createObjectNode().put("model", request.imageModel()).put("prompt", prompt)
                .put("n", 1).put("stream", request.streaming());
        for (String option : OPTIONS) {
            if (tool.hasNonNull(option)) payload.set(option, tool.get(option));
        }
        if (request.streaming() && tool.has("partial_images")) payload.set("partial_images", tool.get("partial_images"));
        if (action == ImageAction.GENERATE) {
            if (tool.hasNonNull("input_image_mask")) throw ResponsesImageRequest.invalid("input_image_mask requires action=edit");
            payload.remove("input_fidelity");
            return new ImageRequest(payload.toString(), ProtocolOperation.INVOKE, "generate", payload);
        }
        if (inputImages.isEmpty()) throw ResponsesImageRequest.invalid("action=edit requires an input image");
        if (inputImages.size() > MAX_IMAGES) throw ResponsesImageRequest.invalid("At most 16 input images are supported");
        List<FilePart> files = new ArrayList<>();
        for (String image : inputImages) files.add(decodeImage("image[]", image));
        JsonNode mask = tool.path("input_image_mask");
        if (!mask.isMissingNode() && !mask.isNull()) files.add(decodeImage("mask", mask.path("image_url").asText()));
        if (files.stream().mapToLong(FilePart::size).sum() > MAX_REQUEST_BYTES) {
            throw ResponsesImageRequest.invalid("Combined input images exceed 100 MiB");
        }
        List<TextField> fields = new ArrayList<>();
        payload.fields().forEachRemaining(field -> fields.add(new TextField(field.getKey(), field.getValue().asText())));
        return new ImageRequest(multipartCodec.encode(new MultipartFormPayload(fields, files)),
                ProtocolOperation.IMAGE_EDITS, "edit", payload);
    }

    private FilePart decodeImage(String name, String dataUrl) {
        int separator = dataUrl.indexOf(";base64,");
        if (!dataUrl.startsWith("data:") || separator < 0) {
            throw ResponsesImageRequest.invalid("Image edits require base64 data URLs; remote URLs and file IDs are not supported");
        }
        String mime = dataUrl.substring(5, separator);
        if (!IMAGE_TYPES.contains(mime)) throw ResponsesImageRequest.invalid("Input images must be PNG, JPEG or WebP");
        String data = dataUrl.substring(separator + 8);
        if (data.length() > ((long) MAX_IMAGE_BYTES + 2) / 3 * 4) {
            throw ResponsesImageRequest.invalid("Input image exceeds 50 MiB");
        }
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(data); }
        catch (IllegalArgumentException exception) { throw ResponsesImageRequest.invalid("Invalid base64 image data"); }
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) throw ResponsesImageRequest.invalid("Invalid input image size");
        return new FilePart(name, ("mask".equals(name) ? "mask" : "image") + "." + mime.substring(6), mime, bytes);
    }

    record ImageRequest(String body, ProtocolOperation operation, String action, ObjectNode options) { }
}
