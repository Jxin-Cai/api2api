package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Converts Anthropic-hosted web search into a client-executed custom tool for Bedrock.
 *
 * <p>Bedrock InvokeModel accepts Claude custom tools but does not execute Anthropic's
 * {@code web_search_20250305} server tool. Keeping the unsupported type makes the entire
 * request invalid. Converting it to the same call shape ({@code web_search({query})}) lets
 * clients with a web-search executor keep the tool loop intact.</p>
 */
final class BedrockWebSearchToolCompatibility {

    private static final String WEB_SEARCH_20250305 = "web_search_20250305";
    private static final String APPROXIMATE_LOCATION = "approximate";
    private static final Set<String> LOCATION_FIELDS = Set.of("city", "region", "country", "timezone");
    private static final String BASE_DESCRIPTION =
            "Search the public web for up-to-date information. Pass a concise search query. "
                    + "The caller executes the search and returns the results.";

    private BedrockWebSearchToolCompatibility() {
    }

    static void normalizeRequest(ObjectNode request) {
        JsonNode tools = request.get("tools");
        if (!(tools instanceof ArrayNode toolArray)) {
            return;
        }
        for (int index = 0; index < toolArray.size(); index++) {
            JsonNode tool = toolArray.get(index);
            if (tool.isObject() && WEB_SEARCH_20250305.equals(tool.path("type").asText(""))) {
                toolArray.set(index, toCustomTool((ObjectNode) tool));
            }
        }
    }

    private static ObjectNode toCustomTool(ObjectNode source) {
        validateKnownFields(source);
        validateToolName(source.get("name"));
        validateAllowedCallers(source.get("allowed_callers"));
        List<String> constraints = new ArrayList<>();
        appendMaxUsesConstraint(source.get("max_uses"), constraints);
        appendDomainConstraint(source, constraints);
        appendLocationConstraint(source.get("user_location"), constraints);

        ObjectNode target = source.objectNode();
        target.put("type", "custom");
        target.put("name", "web_search");
        target.put("description", buildDescription(constraints));

        ObjectNode schema = target.putObject("input_schema");
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("query")
                .put("type", "string")
                .put("description", "The web search query.");
        schema.putArray("required").add("query");
        schema.put("additionalProperties", false);
        return target;
    }

    private static void validateKnownFields(ObjectNode source) {
        Set<String> supportedFields = Set.of(
                "type", "name", "max_uses", "allowed_domains", "blocked_domains", "user_location",
                "allowed_callers"
        );
        source.fieldNames().forEachRemaining(field -> {
            if (!supportedFields.contains(field)) {
                throw new ProtocolConversionException(
                        "Claude web search field '" + field + "' cannot be converted to Bedrock InvokeModel");
            }
        });
    }

    private static void validateToolName(JsonNode name) {
        if (name == null || !name.isTextual() || !"web_search".equals(name.asText())) {
            throw new ProtocolConversionException(
                    "Claude web_search_20250305 tool name must be 'web_search'");
        }
    }

    private static void validateAllowedCallers(JsonNode allowedCallers) {
        if (allowedCallers == null || allowedCallers.isNull()) {
            return;
        }
        if (!allowedCallers.isArray() || allowedCallers.isEmpty()) {
            throw new ProtocolConversionException(
                    "Claude web search allowed_callers must be a non-empty array");
        }
        for (JsonNode caller : allowedCallers) {
            if (!caller.isTextual() || !"direct".equals(caller.asText())) {
                throw new ProtocolConversionException(
                        "Bedrock custom web search only supports allowed_callers=['direct']");
            }
        }
    }

    private static void appendMaxUsesConstraint(JsonNode maxUses, List<String> constraints) {
        if (maxUses == null || maxUses.isNull()) {
            return;
        }
        if (!maxUses.isIntegralNumber() || !maxUses.canConvertToInt() || maxUses.intValue() <= 0) {
            throw new ProtocolConversionException("Claude web search max_uses must be a positive integer");
        }
        constraints.add("Use this tool at most " + maxUses.intValue() + " times for this request.");
    }

    private static void appendDomainConstraint(ObjectNode source, List<String> constraints) {
        JsonNode allowedDomains = source.get("allowed_domains");
        JsonNode blockedDomains = source.get("blocked_domains");
        if (hasValues(allowedDomains) && hasValues(blockedDomains)) {
            throw new ProtocolConversionException(
                    "Claude web search cannot use allowed_domains and blocked_domains together");
        }
        if (allowedDomains != null && !allowedDomains.isNull()) {
            constraints.add("Only use results from these domains: "
                    + domainList(allowedDomains, "allowed_domains") + ".");
        }
        if (blockedDomains != null && !blockedDomains.isNull()) {
            constraints.add("Never use results from these domains: "
                    + domainList(blockedDomains, "blocked_domains") + ".");
        }
    }

    private static boolean hasValues(JsonNode value) {
        return value != null && value.isArray() && !value.isEmpty();
    }

    private static String domainList(JsonNode domains, String fieldName) {
        if (!domains.isArray()) {
            throw new ProtocolConversionException("Claude web search " + fieldName + " must be an array");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode domain : domains) {
            if (!domain.isTextual() || domain.asText().isBlank()) {
                throw new ProtocolConversionException(
                        "Claude web search " + fieldName + " entries must be non-blank strings");
            }
            values.add(domain.asText());
        }
        return String.join(", ", values);
    }

    private static void appendLocationConstraint(JsonNode location, List<String> constraints) {
        if (location == null || location.isNull()) {
            return;
        }
        if (!location.isObject()) {
            throw new ProtocolConversionException("Claude web search user_location must be an object");
        }
        location.fieldNames().forEachRemaining(field -> {
            if (!"type".equals(field) && !LOCATION_FIELDS.contains(field)) {
                throw new ProtocolConversionException(
                        "Claude web search user_location field '" + field + "' is not supported");
            }
        });
        if (!APPROXIMATE_LOCATION.equals(location.path("type").asText(""))) {
            throw new ProtocolConversionException(
                    "Claude web search user_location.type must be 'approximate'");
        }
        boolean hasLocationValue = false;
        for (String field : LOCATION_FIELDS) {
            JsonNode value = location.get(field);
            if (value != null) {
                if (!value.isTextual() || value.asText().isBlank()) {
                    throw new ProtocolConversionException(
                            "Claude web search user_location." + field + " must be a non-blank string");
                }
                hasLocationValue = true;
            }
        }
        if (!hasLocationValue) {
            throw new ProtocolConversionException(
                    "Claude web search user_location requires city, region, country, or timezone");
        }
        constraints.add("Localize results for this approximate user location: " + location + ".");
    }

    private static String buildDescription(List<String> constraints) {
        if (constraints.isEmpty()) {
            return BASE_DESCRIPTION;
        }
        return BASE_DESCRIPTION + " " + String.join(" ", constraints);
    }
}
