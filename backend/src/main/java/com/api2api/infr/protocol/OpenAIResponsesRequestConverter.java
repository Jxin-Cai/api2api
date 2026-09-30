package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingLossiness;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.api2api.infr.protocol.conversion.ProtocolConversionProgram;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Converts client Responses history envelopes into public Responses request items. */
final class OpenAIResponsesRequestConverter implements ProtocolMessageConverter {

    private static final Logger log = LoggerFactory.getLogger(OpenAIResponsesRequestConverter.class);
    private static final ConversionCapability CAPABILITY = ConversionCapability.of(
            true, true, true, true, true, EnumSet.allOf(ContentMappingType.class));
    private static final ProtocolConversionProgram PROGRAM = ProtocolConversionProgram.singleRule(
            ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.REQUEST,
            "Responses client history compatibility", (source, requirement) -> normalizeInputItems(source),
            List.of(FieldMapping.of("input[].author", "input[]",
                    "移除客户端历史封装的 author；保留原生 Responses 字段与工具数据", MappingLossiness.PARTIAL),
                    FieldMapping.of("input[role=assistant].content[].type=input_text", "input[].content[].type=output_text",
                            "assistant 结构化历史使用输出文本类型，并移除仅输入块支持的缓存断点", MappingLossiness.PARTIAL)));

    private final ProtocolJsonSupport json;

    OpenAIResponsesRequestConverter(ProtocolJsonSupport json) {
        this.json = json;
    }

    @Override public ProtocolType sourceProtocol() { return ProtocolType.OPENAI_RESPONSES; }
    @Override public ProtocolType targetProtocol() { return ProtocolType.OPENAI_RESPONSES; }
    @Override public ProtocolConversionDirection direction() { return ProtocolConversionDirection.REQUEST; }
    @Override public ConversionCapability capability() { return CAPABILITY; }
    @Override public ProtocolConversionProgram conversionProgram() { return PROGRAM; }

    @Override
    public ProtocolConversionResult convert(ProtocolPayload payload, ProtocolConversionRequest requirement) {
        if (payload.protocol() != sourceProtocol()) {
            throw new ProtocolConversionException("Responses request converter requires OPENAI_RESPONSES input");
        }
        JsonNode source = json.parse(payload.body(), "Responses request");
        JsonNode target = PROGRAM.execute(source, requirement);
        if (source == target) {
            return ProtocolConversionResult.passthrough(payload);
        }
        return ProtocolConversionResult.of(sourceProtocol(), targetProtocol(),
                json.stringify(target, "Responses request"), true, null);
    }

    /** Shared by converters that produce Responses requests, including opaque history replay. */
    static JsonNode normalizeInputItems(JsonNode request) {
        if (request == null || !request.isObject()) {
            throw new ProtocolConversionException("RESPONSES_REQUEST_MUST_BE_OBJECT");
        }
        JsonNode input = request.path("input");
        if (!input.isArray()) {
            return request;
        }
        ObjectNode normalized = null;
        int normalizedItems = 0;
        for (int index = 0; index < input.size(); index++) {
            if (!(input.get(index) instanceof ObjectNode item)) {
                continue;
            }
            ObjectNode normalizedItem = normalizeInputItem(item);
            if (normalizedItem == item) {
                continue;
            }
            if (normalized == null) {
                normalized = request.deepCopy();
            }
            ((ArrayNode) normalized.path("input")).set(index, normalizedItem);
            normalizedItems++;
        }
        if (normalized != null) {
            log.info("event=responses_request_history_normalized itemCount={}", normalizedItems);
        }
        return normalized == null ? request : normalized;
    }

    private static ObjectNode normalizeInputItem(ObjectNode item) {
        ObjectNode normalized = null;
        // Only strip envelope metadata, never application fields in tool data or schemas.
        if (item.has("author")) {
            normalized = item.deepCopy();
            normalized.remove("author");
        }
        JsonNode content = item.path("content");
        if ("message".equals(item.path("type").asText("message"))
                && "assistant".equals(item.path("role").asText()) && content.isArray()) {
            for (int index = 0; index < content.size(); index++) {
                JsonNode part = content.get(index);
                if (!part.isObject() || !"input_text".equals(part.path("type").asText())) {
                    continue;
                }
                if (normalized == null) {
                    normalized = item.deepCopy();
                }
                ObjectNode text = (ObjectNode) normalized.path("content").get(index);
                text.put("type", "output_text");
                text.remove("prompt_cache_breakpoint");
            }
        }
        return normalized == null ? item : normalized;
    }
}
