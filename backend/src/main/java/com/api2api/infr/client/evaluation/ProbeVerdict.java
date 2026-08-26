package com.api2api.infr.client.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;

/**
 * Verdict of a single probe.
 *
 * <p>The probe service encodes this in one polymorphic {@code passed} field that is {@code true},
 * {@code false}, the string {@code "warning"}, or {@code null} when the probe itself errored. This
 * enum normalizes those four shapes into named cases.
 */
public enum ProbeVerdict {

    PASSED,
    WARNING,
    FAILED,
    /** The probe never produced a verdict, for example because the request errored. */
    UNKNOWN;

    private static final String WARNING_WIRE_VALUE = "warning";

    public static ProbeVerdict of(JsonNode passed) {
        if (passed == null || passed.isNull() || passed.isMissingNode()) {
            return UNKNOWN;
        }
        if (passed.isBoolean()) {
            return passed.asBoolean() ? PASSED : FAILED;
        }
        if (passed.isTextual() && WARNING_WIRE_VALUE.equalsIgnoreCase(passed.asText().trim())) {
            return WARNING;
        }
        return UNKNOWN;
    }

    public JsonNode toJson(ObjectMapper objectMapper) {
        return objectMapper.getNodeFactory().textNode(name().toLowerCase(Locale.ROOT));
    }
}
