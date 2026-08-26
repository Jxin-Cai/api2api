package com.api2api.infr.client.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Compacts a probe report before it is persisted.
 *
 * <p>A raw report is around 100 KB because every probe embeds the full model response and response
 * headers. Those two fields are dropped and the remaining free text is truncated, which keeps the
 * per-probe verdicts, timings and identity findings while shrinking the stored document by roughly
 * an order of magnitude. The untouched original stays reachable through the report URL.
 */
@Component
@RequiredArgsConstructor
public class ProbeReportCompactor {

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final EvaluationProbeProperties properties;

    public String compact(JsonNode report) {
        Objects.requireNonNull(report, "Probe report must not be null");
        ObjectNode summary = objectMapper.createObjectNode();
        copyText(report, summary, "runId");
        copyText(report, summary, "modelId");
        copyText(report, summary, "upstreamFormat");
        copyText(report, summary, "protocolPath");
        copyText(report, summary, "judgeModelId");
        ProbeIdentity.read(report.path("identityAssessment"))
                .ifPresent(identity -> summary.set("identity", compactIdentity(identity)));
        summary.set("channelSignature", compactSignature(report.path("channelSignature")));
        summary.set("probes", compactProbes(probeItems(report)));
        return summary.toString();
    }

    private ObjectNode compactIdentity(ProbeIdentity identity) {
        ObjectNode compacted = objectMapper.createObjectNode();
        if (identity.family() != null) {
            compacted.put("family", identity.family());
        }
        if (identity.model() != null) {
            compacted.put("model", identity.model());
        }
        if (identity.confidence() != null) {
            compacted.put("confidence", identity.confidence());
        }
        if (identity.familyMismatch() != null) {
            compacted.put("familyMismatch", identity.familyMismatch());
        }
        return compacted;
    }

    private static JsonNode probeItems(JsonNode report) {
        JsonNode items = report.path("items");
        return items.isArray() ? items : report.path("results");
    }

    private ObjectNode compactSignature(JsonNode signature) {
        ObjectNode compacted = objectMapper.createObjectNode();
        if (!signature.isObject()) {
            return compacted;
        }
        copyText(signature, compacted, "channel");
        if (signature.hasNonNull("confidence")) {
            compacted.put("confidence", signature.path("confidence").asDouble());
        }
        JsonNode evidence = signature.path("evidence");
        if (evidence.isArray()) {
            ArrayNode compactedEvidence = objectMapper.createArrayNode();
            evidence.forEach(item -> compactedEvidence.add(truncate(item.asText(""))));
            compacted.set("evidence", compactedEvidence);
        }
        return compacted;
    }

    private ArrayNode compactProbes(JsonNode items) {
        ArrayNode compacted = objectMapper.createArrayNode();
        if (!items.isArray()) {
            return compacted;
        }
        for (JsonNode item : items) {
            ObjectNode probe = objectMapper.createObjectNode();
            copyText(item, probe, "probeId");
            copyText(item, probe, "label");
            copyText(item, probe, "group");
            copyText(item, probe, "status");
            probe.put("neutral", item.path("neutral").asBoolean(false));
            probe.set("passed", ProbeVerdict.of(item.path("passed")).toJson(objectMapper));
            putTruncated(probe, "passReason", item.path("passReason"));
            putTruncated(probe, "error", item.path("error"));
            copyNumber(item, probe, "ttftMs");
            copyNumber(item, probe, "durationMs");
            copyNumber(item, probe, "inputTokens");
            copyNumber(item, probe, "outputTokens");
            compacted.add(probe);
        }
        return compacted;
    }

    private void copyText(JsonNode source, ObjectNode target, String field) {
        if (source.hasNonNull(field)) {
            target.put(field, source.path(field).asText());
        }
    }

    private void copyNumber(JsonNode source, ObjectNode target, String field) {
        if (source.hasNonNull(field) && source.path(field).isNumber()) {
            target.put(field, source.path(field).asLong());
        }
    }

    private void putTruncated(ObjectNode target, String field, JsonNode value) {
        if (value.isNull() || value.isMissingNode()) {
            return;
        }
        String text = truncate(value.asText(""));
        if (!text.isBlank()) {
            target.put(field, text);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        int limit = properties.getReportExcerptLength();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }
}
