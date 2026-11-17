package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/** URL citations use a gateway envelope, never a fabricated Anthropic encrypted index. */
final class ResponsesCitationBridge {
    private static final String INDEX_PREFIX = "api2api_url_v1_";

    private ResponsesCitationBridge() {}

    static Optional<ObjectNode> toClaude(ObjectMapper mapper, JsonNode annotation, String text) {
        if (!"url_citation".equals(annotation.path("type").asText())
                || annotation.path("url").asText().isBlank()) {
            return Optional.empty();
        }
        ObjectNode citation = mapper.createObjectNode().put("type", "web_search_result_location")
                .put("url", annotation.path("url").asText())
                .put("cited_text", citedText(annotation, text))
                .put("encrypted_index", INDEX_PREFIX + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(annotation.toString().getBytes(StandardCharsets.UTF_8)));
        citation.set("title", annotation.hasNonNull("title") ? annotation.get("title") : mapper.nullNode());
        return Optional.of(citation);
    }

    static ArrayNode toClaude(ObjectMapper mapper, JsonNode part) {
        ArrayNode citations = mapper.createArrayNode();
        for (JsonNode annotation : part.path("annotations")) {
            toClaude(mapper, annotation, part.path("text").asText()).ifPresent(citations::add);
        }
        return citations;
    }

    static ArrayNode toResponses(ObjectMapper mapper, JsonNode block) {
        ArrayNode annotations = mapper.createArrayNode();
        for (JsonNode citation : block.path("citations")) {
            String index = citation.path("encrypted_index").asText();
            if (!index.startsWith(INDEX_PREFIX)) {
                continue; // Native provider indexes cannot be decoded or translated losslessly.
            }
            try {
                JsonNode annotation = mapper.readTree(Base64.getUrlDecoder().decode(index.substring(INDEX_PREFIX.length())));
                if (annotation == null || !annotation.isObject()
                        || !"url_citation".equals(annotation.path("type").asText())
                        || annotation.path("url").asText().isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_INVALID_CITATION_STATE");
                }
                annotations.add(annotation);
            } catch (IllegalArgumentException | java.io.IOException exception) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_INVALID_CITATION_STATE", exception);
            }
        }
        return annotations;
    }

    private static String citedText(JsonNode annotation, String text) {
        JsonNode startNode = annotation.path("start_index");
        JsonNode endNode = annotation.path("end_index");
        if (!startNode.isIntegralNumber() || !startNode.canConvertToInt()
                || !endNode.isIntegralNumber() || !endNode.canConvertToInt()) {
            return "";
        }
        int start = startNode.intValue();
        int end = endNode.intValue();
        int length = text.codePointCount(0, text.length());
        if (start < 0 || end < start || end > length) {
            return "";
        }
        return text.substring(text.offsetByCodePoints(0, start), text.offsetByCodePoints(0, end));
    }
}
