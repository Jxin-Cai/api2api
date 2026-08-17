package com.api2api.infr.client.provider;

import com.api2api.application.BusinessException;
import com.api2api.application.channel.ProviderModelFetchPort;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OpenAI-compatible provider model fetch adapter.
 */
@Slf4j
@Component
public class ProviderModelFetchAdapter implements ProviderModelFetchPort {

    @NonNull
    private final ProviderSecretResolver providerSecretResolver;

    @NonNull
    private final ProviderHttpClientProperties properties;

    @NonNull
    private final UpstreamHttpHeaderPolicy headerPolicy;

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final UpstreamUrlResolver urlResolver;

    @NonNull
    private final Clock clock;

    private final HttpClient httpClient;

    public ProviderModelFetchAdapter(
            ProviderSecretResolver providerSecretResolver,
            ProviderHttpClientProperties properties,
            UpstreamHttpHeaderPolicy headerPolicy,
            ObjectMapper objectMapper,
            UpstreamUrlResolver urlResolver,
            Clock clock
    ) {
        this.providerSecretResolver = Objects.requireNonNull(providerSecretResolver, "Provider secret resolver must not be null");
        this.properties = Objects.requireNonNull(properties, "Provider HTTP client properties must not be null");
        this.headerPolicy = Objects.requireNonNull(headerPolicy, "Upstream HTTP header policy must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        this.urlResolver = Objects.requireNonNull(urlResolver, "Upstream URL resolver must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public List<ChannelModelSupport> fetchModels(
            ProviderChannelId channelId,
            ProviderHost host,
            ProviderKeyRef keyRef,
            ProviderModelsPath modelsPath,
            Set<ProtocolType> upstreamProtocols,
            RoutePriority defaultPriority
    ) {
        Objects.requireNonNull(channelId, "Provider channel id must not be null");
        Objects.requireNonNull(host, "Provider host must not be null");
        Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        Objects.requireNonNull(modelsPath, "Provider models path must not be null");
        Objects.requireNonNull(upstreamProtocols, "Upstream protocols must not be null");
        Objects.requireNonNull(defaultPriority, "Default route priority must not be null");
        if (upstreamProtocols.isEmpty()) {
            throw new BusinessException(
                    "PROVIDER_MODELS_UPSTREAM_PROTOCOLS_EMPTY",
                    "渠道未配置上游调用协议，无法验证模型列表"
            );
        }

        String secret = providerSecretResolver.resolve(keyRef);
        if (secret == null || secret.isBlank()) {
            throw new BusinessException("PROVIDER_MODELS_AUTH_FAILED", "渠道未配置可用的 API Key");
        }
        String effectiveModelsPath = ProviderModelsPath.DEFAULT.value();
        URI uri = OutboundUriGuard.verify(
                URI.create(urlResolver.resolve(host.resolvePath(effectiveModelsPath).value())),
                properties.isAllowInsecureHosts());
        ProtocolType headerProtocol = upstreamProtocols.stream()
                .filter(ProtocolType::isClientFacing)
                .findFirst()
                .orElse(ProtocolType.OPENAI_CHAT_COMPLETIONS);
        Map<String, String> headers = headerPolicy.buildHeaders(headerProtocol, Map.of(), secret, false);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(properties.getModelsReadTimeout())
                .GET();
        headers.forEach(requestBuilder::header);

        try {
            HttpResponse<String> response = sendWithRetry(requestBuilder.build());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Provider model fetch failed: status={}, uri={}", response.statusCode(), uri);
                throw providerModelsHttpException(response.statusCode(), uri, response.body());
            }
            return toModelSupports(response.body(), upstreamProtocols, defaultPriority);
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpTimeoutException exception) {
            throw new BusinessException(
                    "PROVIDER_MODELS_TIMEOUT",
                    "请求上游模型列表超时：" + uri,
                    exception
            );
        } catch (IOException exception) {
            throw new BusinessException(
                    "PROVIDER_MODELS_IO_ERROR",
                    "请求上游模型列表失败：" + safeIoMessage(exception, uri),
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("PROVIDER_MODELS_INTERRUPTED", "请求上游模型列表被中断：" + uri, exception);
        }
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
        int maxAttempts = Math.max(1, properties.getModelsMaxRetries() + 1);
        IOException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException exception) {
                throw exception;
            } catch (IOException exception) {
                lastException = exception;
                if (attempt == maxAttempts) {
                    throw exception;
                }
            }
        }
        throw lastException == null ? new IOException("Provider model fetch failed") : lastException;
    }

    private BusinessException providerModelsHttpException(int statusCode, URI uri, String responseBody) {
        String requestTarget = uri == null ? ProviderModelsPath.DEFAULT.value() : uri.toString();
        String upstreamReason = extractUpstreamError(responseBody);
        String suffix = upstreamReason.isBlank() ? "" : "：" + upstreamReason;
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(
                    "PROVIDER_MODELS_AUTH_FAILED",
                    "上游认证失败（HTTP " + statusCode + "），请求 " + requestTarget + " 时被拒绝" + suffix
            );
        }
        if (statusCode == 404) {
            return new BusinessException(
                    "PROVIDER_MODELS_PATH_NOT_FOUND",
                    "未找到模型列表接口（HTTP 404），默认请求 " + requestTarget + suffix
            );
        }
        return new BusinessException(
                "PROVIDER_MODELS_HTTP_" + statusCode,
                "上游返回错误（HTTP " + statusCode + "），请求 " + requestTarget + suffix
        );
    }

    private String extractUpstreamError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = firstNonBlank(
                    root.path("error").path("message").asText(""),
                    root.path("message").asText(""),
                    root.path("error").asText(""),
                    root.path("detail").asText("")
            );
            return truncateError(message);
        } catch (JsonProcessingException exception) {
            log.debug("Provider model fetch error body is not JSON", exception);
            return truncateError(responseBody);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String truncateError(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 240);
    }

    private static String safeIoMessage(IOException exception, URI uri) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            return uri.toString();
        }
        return uri + "，" + truncateError(detail);
    }

    private List<ChannelModelSupport> toModelSupports(
            String responseBody,
            Set<ProtocolType> upstreamProtocols,
            RoutePriority defaultPriority
    ) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode modelSummaries = root.path("modelSummaries");
        if (modelSummaries.isArray()) {
            return toBedrockModelSupports(modelSummaries, upstreamProtocols, defaultPriority);
        }

        JsonNode data = modelEntries(root);
        if (!data.isArray()) {
            throw new BusinessException("PROVIDER_MODELS_RESPONSE_INVALID", "上游模型列表响应格式无效，未返回 data/models 列表");
        }
        Set<String> uniqueModelIds = new LinkedHashSet<>();
        List<ModelName> modelNames = new ArrayList<>();
        for (JsonNode item : data) {
            String id = modelId(item);
            if (id.isBlank() || !uniqueModelIds.add(id)) {
                continue;
            }
            modelNames.add(ModelName.of(id));
        }
        return buildModelSupports(modelNames, upstreamProtocols, defaultPriority).stream()
                .sorted(Comparator.comparing(model -> model.requestedModel().value()))
                .toList();
    }

    private JsonNode modelEntries(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        boolean hasData = root.path("data").isArray();
        boolean hasModels = root.path("models").isArray();
        if (!hasData && !hasModels) {
            return root.path("data");
        }
        ArrayNode entries = objectMapper.createArrayNode();
        if (hasData) {
            entries.addAll((ArrayNode) root.path("data"));
        }
        if (hasModels) {
            entries.addAll((ArrayNode) root.path("models"));
        }
        return entries;
    }

    private String modelId(JsonNode item) {
        if (item.isTextual()) {
            return normalizeModelId(item.asText());
        }
        for (String field : List.of("id", "slug", "name", "model")) {
            String value = item.path(field).asText("").trim();
            if (!value.isBlank()) {
                return normalizeModelId(value);
            }
        }
        return "";
    }

    private String normalizeModelId(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.startsWith("models/") ? normalized.substring("models/".length()) : normalized;
    }

    private List<ChannelModelSupport> toBedrockModelSupports(
            JsonNode modelSummaries,
            Set<ProtocolType> upstreamProtocols,
            RoutePriority defaultPriority
    ) {
        List<ModelName> modelNames = new ArrayList<>();
        for (JsonNode item : modelSummaries) {
            String modelId = item.path("modelId").asText("").trim();
            if (modelId.isBlank()) {
                continue;
            }
            JsonNode inferenceTypes = item.path("inferenceTypesSupported");
            if (inferenceTypes.isArray()) {
                boolean supportsOnDemand = false;
                for (JsonNode inferenceType : inferenceTypes) {
                    if ("ON_DEMAND".equals(inferenceType.asText(""))) {
                        supportsOnDemand = true;
                        break;
                    }
                }
                if (!supportsOnDemand) {
                    continue;
                }
            }
            modelNames.add(ModelName.of(modelId));
        }
        return buildModelSupports(modelNames, upstreamProtocols, defaultPriority);
    }

    private List<ChannelModelSupport> buildModelSupports(
            List<ModelName> modelNames,
            Set<ProtocolType> upstreamProtocols,
            RoutePriority defaultPriority
    ) {
        if (modelNames.isEmpty()) {
            throw new BusinessException("PROVIDER_MODELS_EMPTY", "上游未返回任何模型");
        }
        Set<ProtocolType> uniqueProtocols = new LinkedHashSet<>(upstreamProtocols);
        List<ChannelModelSupport> supports = new ArrayList<>();
        long idBase = Instant.now(clock).toEpochMilli() * 1_000L;
        long index = 1L;
        for (ModelName modelName : modelNames) {
            for (ProtocolType protocol : uniqueProtocols) {
                supports.add(ChannelModelSupport.create(
                        ChannelModelSupportId.of(idBase + index++),
                        modelName,
                        modelName,
                        protocol,
                        defaultPriority,
                        false,
                        ModelSupportSource.FETCHED,
                        Instant.now(clock)
                ));
            }
        }
        return supports;
    }
}
