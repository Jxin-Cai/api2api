package com.api2api.infr.client.evaluation;

import com.api2api.application.BusinessException;
import com.api2api.application.evaluation.ChannelEvaluationProbePort;
import com.api2api.application.evaluation.ProbeRunSnapshot;
import com.api2api.application.evaluation.ProbeSubmission;
import com.api2api.domain.evaluation.model.EvaluationOutcome;
import com.api2api.domain.evaluation.model.EvaluationScore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP adapter to the BazaarLink probe service.
 *
 * <p>Runs are submitted asynchronously ({@code sync=false}) and later polled. The adapter never logs
 * the channel key, only the run identifier and HTTP status.
 */
@Slf4j
@Component
public class ChannelEvaluationProbeAdapter implements ChannelEvaluationProbePort {

    private static final String CONTENT_TYPE = "application/json";

    @NonNull
    private final EvaluationProbeProperties properties;

    @NonNull
    private final ProbeReportCompactor reportCompactor;

    @NonNull
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public ChannelEvaluationProbeAdapter(
            EvaluationProbeProperties properties,
            ProbeReportCompactor reportCompactor,
            ObjectMapper objectMapper
    ) {
        this.properties = Objects.requireNonNull(properties, "Evaluation probe properties must not be null");
        this.reportCompactor = Objects.requireNonNull(reportCompactor, "Probe report compactor must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public String submit(ProbeSubmission submission) {
        Objects.requireNonNull(submission, "Probe submission must not be null");
        String secret = submission.keyRef().value();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("baseUrl", submission.host().value());
        body.put("apiKey", secret);
        body.put("modelId", submission.modelId().value());
        body.put("claimedModel", submission.modelId().value());
        body.put("upstreamFormat", submission.upstreamFormat().wireValue());
        body.put("sync", false);
        JsonNode response = post(properties.runEndpoint(), body);
        String runId = text(response, "runId");
        if (runId == null || runId.isBlank()) {
            throw new BusinessException("EVALUATION_PROBE_RUN_ID_MISSING", "探测服务未返回运行标识");
        }
        return runId;
    }

    @Override
    public ProbeRunSnapshot fetch(String providerRunId) {
        if (providerRunId == null || providerRunId.isBlank()) {
            throw new IllegalArgumentException("Provider run id must not be blank");
        }
        JsonNode live = get(properties.runEndpoint(providerRunId), true);
        if (live != null) {
            ProbeRunSnapshot snapshot = snapshotOf(live, providerRunId);
            if (snapshot != null) {
                return snapshot;
            }
        }
        JsonNode history = get(properties.historyEndpoint(providerRunId), false);
        ProbeRunSnapshot snapshot = snapshotOf(history, providerRunId);
        if (snapshot == null) {
            throw new BusinessException("EVALUATION_PROBE_RESULT_MISSING", "探测服务未返回可识别的运行结果");
        }
        return snapshot;
    }

    private ProbeRunSnapshot snapshotOf(JsonNode report, String providerRunId) {
        if (report == null || report.isMissingNode() || report.isNull()) {
            return null;
        }
        String status = text(report, "status");
        if (status == null) {
            if (report.hasNonNull("score") || report.hasNonNull("completedAt")) {
                return ProbeRunSnapshot.succeeded(toOutcome(report, providerRunId));
            }
            return ProbeRunSnapshot.running();
        }
        String normalized = status.trim().toLowerCase();
        if ("running".equals(normalized) || "pending".equals(normalized) || "queued".equals(normalized)) {
            return ProbeRunSnapshot.running();
        }
        if ("failed".equals(normalized) || "error".equals(normalized) || "stopped".equals(normalized)) {
            return ProbeRunSnapshot.failed(firstNonBlank(
                    text(report, "error"),
                    text(report, "errorMessage"),
                    text(report, "message"),
                    "探测服务报告失败"
            ));
        }
        if ("completed".equals(normalized) || "succeeded".equals(normalized) || "success".equals(normalized)) {
            return ProbeRunSnapshot.succeeded(toOutcome(report, providerRunId));
        }
        return ProbeRunSnapshot.running();
    }

    private EvaluationOutcome toOutcome(JsonNode report, String providerRunId) {
        BigDecimal rawScore = decimal(report.path("score"));
        BigDecimal rawMax = decimal(report.path("scoreMax"));
        if (rawScore == null) {
            throw new BusinessException("EVALUATION_PROBE_SCORE_MISSING", "探测服务成功响应缺少评分");
        }
        EvaluationScore score = EvaluationScore.of(rawScore, rawMax == null ? BigDecimal.valueOf(100) : rawMax);
        ProbeIdentity identity = ProbeIdentity.read(report.path("identityAssessment")).orElse(null);
        ProbeCounts counts = ProbeCounts.of(probeItems(report));
        return EvaluationOutcome.builder()
                .score(score)
                .detectedFamily(identity == null ? null : identity.family())
                .detectedModel(identity == null ? null : identity.model())
                .detectedConfidence(identity == null ? null : identity.confidence())
                .familyMismatch(identity == null ? null : identity.familyMismatch())
                .channelSignature(text(report.path("channelSignature"), "channel"))
                .reportUrl(properties.historyEndpoint(providerRunId))
                .passedProbeCount(counts.passed)
                .warningProbeCount(counts.warning)
                .failedProbeCount(counts.failed)
                .totalInputTokens(nullableLong(report.path("totalInputTokens")))
                .totalOutputTokens(nullableLong(report.path("totalOutputTokens")))
                .reportSummary(reportCompactor.compact(report))
                .completedAt(instant(report.path("completedAt")))
                .build();
    }

    private JsonNode post(String endpoint, JsonNode body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(properties.getRequestTimeout())
                    .header("Content-Type", CONTENT_TYPE)
                    .header("Accept", CONTENT_TYPE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            return send(request, endpoint, false);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("EVALUATION_PROBE_REQUEST_INVALID", "探测服务请求序列化失败", exception);
        }
    }

    private JsonNode get(String endpoint, boolean allowNotFound) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(properties.getRequestTimeout())
                .header("Accept", CONTENT_TYPE)
                .GET()
                .build();
        return send(request, endpoint, allowNotFound);
    }

    private JsonNode send(HttpRequest request, String endpoint, boolean allowNotFound) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (allowNotFound && status == 404) {
                return null;
            }
            if (status < 200 || status >= 300) {
                log.warn("Evaluation probe HTTP {} at {}", status, endpoint);
                throw probeHttpException(status, extractError(response.body()));
            }
            if (response.body() == null || response.body().isBlank()) {
                throw new BusinessException("EVALUATION_PROBE_RESPONSE_EMPTY", "探测服务返回空响应");
            }
            return objectMapper.readTree(response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpTimeoutException exception) {
            throw new BusinessException("EVALUATION_PROBE_TIMEOUT", "请求探测服务超时", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("EVALUATION_PROBE_INTERRUPTED", "请求探测服务被中断", exception);
        } catch (IOException exception) {
            throw new BusinessException("EVALUATION_PROBE_IO_ERROR", "请求探测服务失败", exception);
        }
    }

    private BusinessException probeHttpException(int statusCode, String reason) {
        String suffix = reason.isBlank() ? "" : "：" + reason;
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException("EVALUATION_PROBE_AUTH_FAILED", "探测服务认证失败（HTTP " + statusCode + "）" + suffix);
        }
        if (statusCode == 404) {
            return new BusinessException("EVALUATION_PROBE_NOT_FOUND", "探测服务未找到该运行（HTTP 404）" + suffix);
        }
        if (statusCode == 429) {
            return new BusinessException("EVALUATION_PROBE_RATE_LIMITED", "探测服务限流（HTTP 429）" + suffix);
        }
        return new BusinessException("EVALUATION_PROBE_HTTP_" + statusCode, "探测服务返回错误（HTTP " + statusCode + "）" + suffix);
    }

    private String extractError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return firstNonBlank(
                    text(root.path("error"), "message"),
                    text(root, "message"),
                    text(root, "error"),
                    text(root, "detail")
            );
        } catch (JsonProcessingException exception) {
            log.debug("Evaluation probe error body is not JSON", exception);
            return truncate(responseBody);
        }
    }

    private static JsonNode probeItems(JsonNode report) {
        JsonNode items = report.path("items");
        return items.isArray() ? items : report.path("results");
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }

    private static BigDecimal decimal(JsonNode node) {
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }

    private static Long nullableLong(JsonNode node) {
        return node != null && node.isNumber() ? node.longValue() : null;
    }

    private static Instant instant(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode() || !node.isTextual()) {
            return null;
        }
        try {
            return Instant.parse(node.asText());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return truncate(value.trim());
            }
        }
        return "";
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
    }

    private static final class ProbeCounts {
        private final int passed;
        private final int warning;
        private final int failed;

        private ProbeCounts(int passed, int warning, int failed) {
            this.passed = passed;
            this.warning = warning;
            this.failed = failed;
        }

        private static ProbeCounts of(JsonNode items) {
            int passed = 0;
            int warning = 0;
            int failed = 0;
            if (!items.isArray()) {
                return new ProbeCounts(0, 0, 0);
            }
            for (JsonNode item : items) {
                switch (ProbeVerdict.of(item.path("passed"))) {
                    case PASSED -> passed++;
                    case WARNING -> warning++;
                    case FAILED -> failed++;
                    case UNKNOWN -> {
                        if ("error".equalsIgnoreCase(item.path("status").asText(""))) {
                            failed++;
                        }
                    }
                }
            }
            return new ProbeCounts(passed, warning, failed);
        }
    }
}
