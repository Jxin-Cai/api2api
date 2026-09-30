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
                    "移除客户端历史封装的 author；保留原生 Responses 字段与工具数据", MappingLossiness.PARTIAL)));

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
        int removedFields = 0;
        for (int index = 0; index < input.size(); index++) {
            // Do not recurse into tool arguments, output data, metadata or JSON Schemas:
            // an application property named author is valid in all of those locations.
            if (input.get(index).isObject() && input.get(index).has("author")) {
                if (normalized == null) {
                    normalized = request.deepCopy();
                }
                ((ObjectNode) normalized.path("input").get(index)).remove("author");
                removedFields++;
            }
        }
        if (normalized != null) {
            log.info("event=responses_request_history_normalized field=author removedCount={}", removedFields);
        }
        return normalized == null ? request : normalized;
    }
}
