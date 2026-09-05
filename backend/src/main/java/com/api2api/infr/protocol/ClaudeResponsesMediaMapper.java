package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Maps Claude Messages image/document blocks onto OpenAI Responses
 * {@code input_image}/{@code input_file} parts (and back), preserving MIME type,
 * filename and inline payload that the two protocols name differently.
 *
 * <p>Responses treats {@code file_id}/{@code file_url}/{@code filename} as mutually
 * exclusive on a single part, so document title/context that cannot live on the
 * file part are emitted as a sibling {@code input_text} instead of being dropped.
 */
final class ClaudeResponsesMediaMapper {

    private static final String DEFAULT_DOCUMENT_MEDIA_TYPE = "application/pdf";
    private static final String DEFAULT_TEXT_MEDIA_TYPE = "text/plain";
    private static final String DEFAULT_IMAGE_MEDIA_TYPE = "image/png";
    private static final String DATA_URI_BASE64_MARKER = ";base64,";
    private static final Pattern FILENAME_EXTENSION = Pattern.compile(".*\\.[A-Za-z0-9]{1,8}$");

    private static final Map<String, String> MEDIA_TYPE_TO_EXTENSION = Map.ofEntries(
            Map.entry("application/pdf", "pdf"),
            Map.entry("text/plain", "txt"),
            Map.entry("text/markdown", "md"),
            Map.entry("text/html", "html"),
            Map.entry("text/csv", "csv"),
            Map.entry("text/tab-separated-values", "tsv"),
            Map.entry("text/xml", "xml"),
            Map.entry("application/xml", "xml"),
            Map.entry("application/json", "json"),
            Map.entry("application/msword", "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-powerpoint", "ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
            Map.entry("application/vnd.ms-excel", "xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
            Map.entry("application/rtf", "rtf"),
            Map.entry("text/rtf", "rtf"),
            Map.entry("application/vnd.oasis.opendocument.text", "odt"),
            Map.entry("image/png", "png"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/webp", "webp")
    );

    private static final Map<String, String> EXTENSION_TO_MEDIA_TYPE = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/markdown"),
            Map.entry("markdown", "text/markdown"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("csv", "text/csv"),
            Map.entry("tsv", "text/tab-separated-values"),
            Map.entry("xml", "application/xml"),
            Map.entry("json", "application/json"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp")
    );

    private ClaudeResponsesMediaMapper() {
    }

    static Optional<ObjectNode> addClaudeImage(ProtocolJsonSupport json, ArrayNode content, JsonNode block) {
        JsonNode source = block.get("source");
        if (source == null || source.isNull()) {
            return Optional.empty();
        }
        ObjectNode image = json.objectNode();
        image.put("type", "input_image");
        String sourceType = source.path("type").asText("base64");
        switch (sourceType) {
            case "url" -> image.put("image_url", source.path("url").asText(""));
            case "file" -> {
                String fileId = source.path("file_id").asText("");
                if (fileId.isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_IMAGE_FILE_ID_REQUIRED");
                }
                image.put("file_id", fileId);
            }
            case "base64" -> {
                String mediaType = source.path("media_type").asText(DEFAULT_IMAGE_MEDIA_TYPE);
                String data = source.path("data").asText("");
                if (data.isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_IMAGE_DATA_REQUIRED");
                }
                image.put("image_url", toDataUri(mediaType, data));
            }
            default -> throw new ProtocolConversionException(
                    "CLAUDE_RESPONSES_UNSUPPORTED_IMAGE_SOURCE: " + sourceType);
        }
        image.put("detail", "auto");
        content.add(image);
        return Optional.of(image);
    }

    static Optional<ObjectNode> addClaudeDocument(ProtocolJsonSupport json, ArrayNode content, JsonNode block) {
        JsonNode source = block.path("source");
        String type = source.path("type").asText("");
        if ("content".equals(type)) {
            return addClaudeContentDocument(json, content, block);
        }
        ObjectNode file = json.objectNode();
        file.put("type", "input_file");
        boolean filenameAttached = false;
        switch (type) {
            case "base64" -> {
                InlineFile inline = inlineFileFromBase64(block, source);
                file.put("filename", inline.filename());
                file.put("file_data", inline.dataUri());
                filenameAttached = true;
            }
            case "text" -> {
                InlineFile inline = inlineFileFromText(block, source);
                file.put("filename", inline.filename());
                file.put("file_data", inline.dataUri());
                filenameAttached = true;
            }
            case "url" -> file.put("file_url", source.path("url").asText(""));
            case "file" -> {
                String fileId = source.path("file_id").asText("");
                if (fileId.isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_DOCUMENT_FILE_ID_REQUIRED");
                }
                file.put("file_id", fileId);
            }
            default -> throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_DOCUMENT_SOURCE: " + type);
        }
        content.add(file);
        addCompanionText(json, content, block, filenameAttached);
        return Optional.of(file);
    }

    static ObjectNode imagePartToClaude(ProtocolJsonSupport json, JsonNode part) {
        ObjectNode image = json.objectNode();
        image.put("type", "image");
        ObjectNode source = json.objectNode();
        if (part.hasNonNull("file_id") && part.path("image_url").asText("").isBlank()) {
            source.put("type", "file");
            source.put("file_id", part.path("file_id").asText(""));
        } else {
            String imageUrl = part.path("image_url").asText("");
            if (imageUrl.isBlank()) {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_IMAGE_URL_REQUIRED");
            }
            if (imageUrl.startsWith("data:")) {
                DataUri dataUri = DataUri.parseOrRaw(imageUrl, DEFAULT_IMAGE_MEDIA_TYPE);
                source.put("type", "base64");
                source.put("media_type", dataUri.mediaType());
                source.put("data", dataUri.payload());
            } else {
                source.put("type", "url");
                source.put("url", imageUrl);
            }
        }
        image.set("source", source);
        return image;
    }

    static ObjectNode filePartToClaude(ProtocolJsonSupport json, JsonNode part) {
        ObjectNode document = json.objectNode();
        document.put("type", "document");
        ObjectNode source = json.objectNode();
        String filename = part.path("filename").asText("");
        if (part.hasNonNull("file_id")) {
            source.put("type", "file");
            source.put("file_id", part.path("file_id").asText(""));
        } else if (part.hasNonNull("file_url")) {
            source.put("type", "url");
            source.put("url", part.path("file_url").asText(""));
        } else if (part.hasNonNull("file_data")) {
            String fallbackMediaType = mediaTypeFromFilename(filename, DEFAULT_DOCUMENT_MEDIA_TYPE);
            DataUri dataUri = DataUri.parseOrRaw(part.path("file_data").asText(""), fallbackMediaType);
            source.put("type", "base64");
            source.put("media_type", dataUri.mediaType());
            source.put("data", dataUri.payload());
        } else {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_FILE_SOURCE_REQUIRED");
        }
        document.set("source", source);
        if (!filename.isBlank()) {
            document.put("title", filename);
        }
        return document;
    }

    static ObjectNode toChatFilePart(ProtocolJsonSupport json, JsonNode documentBlock) {
        JsonNode source = documentBlock.path("source");
        String sourceType = source.path("type").asText("");
        ObjectNode part = json.objectNode();
        part.put("type", "file");
        ObjectNode file = json.objectNode();
        if ("file".equals(sourceType)) {
            file.put("file_id", source.path("file_id").asText(""));
        } else if ("base64".equals(sourceType) || "text".equals(sourceType)) {
            InlineFile inline = "text".equals(sourceType)
                    ? inlineFileFromText(documentBlock, source)
                    : inlineFileFromBase64(documentBlock, source);
            file.put("filename", inline.filename());
            file.put("file_data", inline.dataUri());
        } else {
            throw new ProtocolConversionException("CLAUDE_CHAT_DOCUMENT_SOURCE_NOT_SUPPORTED: " + sourceType);
        }
        part.set("file", file);
        return part;
    }

    static ObjectNode chatFileToClaudeDocument(ProtocolJsonSupport json, JsonNode file) {
        ObjectNode document = json.objectNode();
        document.put("type", "document");
        ObjectNode source = json.objectNode();
        String filename = file.path("filename").asText("");
        if (file.hasNonNull("file_id")) {
            source.put("type", "file");
            source.put("file_id", file.path("file_id").asText(""));
        } else if (file.hasNonNull("file_data")) {
            String fallbackMediaType = mediaTypeFromFilename(filename, DEFAULT_DOCUMENT_MEDIA_TYPE);
            DataUri dataUri = DataUri.parseOrRaw(file.path("file_data").asText(""), fallbackMediaType);
            source.put("type", "base64");
            source.put("media_type", dataUri.mediaType());
            source.put("data", dataUri.payload());
        } else {
            throw new ProtocolConversionException("OPENAI_CHAT_CLAUDE_FILE_SOURCE_REQUIRED");
        }
        document.set("source", source);
        if (!filename.isBlank()) {
            document.put("title", filename);
        }
        return document;
    }

    static ObjectNode chatFileToResponsesInputFile(ProtocolJsonSupport json, JsonNode file) {
        ObjectNode mapped = json.objectNode();
        mapped.put("type", "input_file");
        if (file.hasNonNull("file_id")) {
            mapped.put("file_id", file.path("file_id").asText(""));
            return mapped;
        }
        if (file.hasNonNull("file_url")) {
            mapped.put("file_url", file.path("file_url").asText(""));
            return mapped;
        }
        if (!file.hasNonNull("file_data")) {
            throw new ProtocolConversionException("OPENAI_CHAT_RESPONSES_FILE_SOURCE_REQUIRED");
        }
        String filename = file.path("filename").asText("");
        String fallbackMediaType = mediaTypeFromFilename(filename, DEFAULT_DOCUMENT_MEDIA_TYPE);
        DataUri dataUri = DataUri.parseOrRaw(file.path("file_data").asText(""), fallbackMediaType);
        mapped.put("filename", filename.isBlank() ? "document." + extensionForMediaType(dataUri.mediaType()) : filename);
        mapped.put("file_data", dataUri.encode());
        return mapped;
    }

    private static Optional<ObjectNode> addClaudeContentDocument(
            ProtocolJsonSupport json,
            ArrayNode content,
            JsonNode block
    ) {
        addCompanionText(json, content, block, false);
        JsonNode nested = block.path("source").get("content");
        if (nested == null || !nested.isArray() || nested.isEmpty()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_DOCUMENT_CONTENT_REQUIRED");
        }
        ObjectNode lastCacheable = null;
        for (JsonNode nestedBlock : nested) {
            String nestedType = nestedBlock.path("type").asText("");
            switch (nestedType) {
                case "text" -> {
                    ObjectNode text = json.objectNode();
                    text.put("type", "input_text");
                    text.put("text", nestedBlock.path("text").asText(""));
                    content.add(text);
                    lastCacheable = text;
                }
                case "image" -> lastCacheable = addClaudeImage(json, content, nestedBlock).orElse(lastCacheable);
                default -> throw new ProtocolConversionException(
                        "CLAUDE_RESPONSES_UNSUPPORTED_DOCUMENT_CONTENT: " + nestedType);
            }
        }
        return Optional.ofNullable(lastCacheable);
    }

    private static void addCompanionText(
            ProtocolJsonSupport json,
            ArrayNode content,
            JsonNode block,
            boolean filenameAttached
    ) {
        String title = block.path("title").asText("").trim();
        String context = block.path("context").asText("").trim();
        StringBuilder text = new StringBuilder();
        if (!filenameAttached && !title.isBlank()) {
            text.append("Document: ").append(title);
        }
        if (!context.isBlank()) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(context);
        }
        if (text.isEmpty()) {
            return;
        }
        ObjectNode part = json.objectNode();
        part.put("type", "input_text");
        part.put("text", text.toString());
        content.add(part);
    }

    private static InlineFile inlineFileFromBase64(JsonNode block, JsonNode source) {
        String data = source.path("data").asText("");
        if (data.isBlank()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_DOCUMENT_DATA_REQUIRED");
        }
        String mediaType = firstNonBlank(
                source.path("media_type").asText(""),
                mediaTypeFromFilename(block.path("title").asText(""), DEFAULT_DOCUMENT_MEDIA_TYPE));
        DataUri dataUri = DataUri.parseOrRaw(data, mediaType);
        return new InlineFile(filenameFor(block, dataUri.mediaType()), dataUri.encode());
    }

    private static InlineFile inlineFileFromText(JsonNode block, JsonNode source) {
        byte[] bytes = source.path("data").asText("").getBytes(StandardCharsets.UTF_8);
        String mediaType = firstNonBlank(source.path("media_type").asText(""), DEFAULT_TEXT_MEDIA_TYPE);
        String payload = java.util.Base64.getEncoder().encodeToString(bytes);
        return new InlineFile(filenameFor(block, mediaType), toDataUri(mediaType, payload));
    }

    static String filenameFor(JsonNode block, String mediaType) {
        String title = sanitizeFilename(block.path("title").asText("").trim());
        String extension = extensionForMediaType(mediaType);
        if (title.isBlank()) {
            return "document." + extension;
        }
        if (FILENAME_EXTENSION.matcher(title).matches()) {
            return title;
        }
        return title + "." + extension;
    }

    static String extensionForMediaType(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            return "pdf";
        }
        String normalized = mediaType.toLowerCase(Locale.ROOT).strip();
        String mapped = MEDIA_TYPE_TO_EXTENSION.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        int slash = normalized.lastIndexOf('/');
        String subtype = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        String candidate = subtype.replaceAll("[^a-z0-9]+", "");
        return candidate.isBlank() ? "bin" : candidate;
    }

    static String mediaTypeFromFilename(String filename, String fallback) {
        if (filename == null) {
            return fallback;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return fallback;
        }
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return EXTENSION_TO_MEDIA_TYPE.getOrDefault(extension, fallback);
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        String stripped = filename.replace('\\', '/');
        int slash = stripped.lastIndexOf('/');
        if (slash >= 0) {
            stripped = stripped.substring(slash + 1);
        }
        return stripped.replaceAll("[\\p{Cntrl}]", "").strip();
    }

    private static String toDataUri(String mediaType, String payload) {
        return new DataUri(mediaType, payload).encode();
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private record InlineFile(String filename, String dataUri) {
    }

    private record DataUri(String mediaType, String payload) {

        String encode() {
            return "data:" + mediaType + DATA_URI_BASE64_MARKER + payload;
        }

        static DataUri parseOrRaw(String value, String fallbackMediaType) {
            Objects.requireNonNull(fallbackMediaType, "fallbackMediaType");
            if (value == null || value.isBlank()) {
                return new DataUri(fallbackMediaType, "");
            }
            if (!value.startsWith("data:")) {
                return new DataUri(fallbackMediaType, value);
            }
            int separator = value.indexOf(DATA_URI_BASE64_MARKER);
            if (separator < 0) {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_FILE_DATA_URI_INVALID");
            }
            String mediaType = value.substring("data:".length(), separator).strip();
            String payload = value.substring(separator + DATA_URI_BASE64_MARKER.length());
            return new DataUri(mediaType.isBlank() ? fallbackMediaType : mediaType, payload);
        }
    }
}
