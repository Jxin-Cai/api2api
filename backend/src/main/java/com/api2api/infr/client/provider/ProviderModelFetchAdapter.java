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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import org.springframework.stereotype.Component;

/**
 * OpenAI-compatible provider model fetch adapter.
 */
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
            throw new BusinessException("PROVIDER_MODELS_UPSTREAM_PROTOCOLS_EMPTY");
        }

        String secret = providerSecretResolver.resolve(keyRef);
        URI uri = URI.create(urlResolver.resolve(host.resolvePath(modelsPath.value()).value()));
        ProtocolType headerProtocol = upstreamProtocols.iterator().next();
        Map<String, String> headers = headerPolicy.buildHeaders(headerProtocol, Map.of(), secret, false);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(properties.getModelsReadTimeout())
                .GET();
        headers.forEach(requestBuilder::header);

        try {
            HttpResponse<String> response = sendWithRetry(requestBuilder.build());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerModelsHttpException(response.statusCode(), modelsPath);
            }
            return toModelSupports(response.body(), upstreamProtocols, defaultPriority);
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpTimeoutException exception) {
            throw new BusinessException("PROVIDER_MODELS_TIMEOUT", exception);
        } catch (IOException exception) {
            throw new BusinessException("PROVIDER_MODELS_IO_ERROR", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("PROVIDER_MODELS_INTERRUPTED", exception);
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

    private BusinessException providerModelsHttpException(int statusCode, ProviderModelsPath modelsPath) {
        Objects.requireNonNull(modelsPath, "Provider models path must not be null");
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException("PROVIDER_MODELS_AUTH_FAILED");
        }
        if (statusCode == 404) {
            return new BusinessException("PROVIDER_MODELS_PATH_NOT_FOUND");
        }
        return new BusinessException("PROVIDER_MODELS_HTTP_" + statusCode);
    }

    private List<ChannelModelSupport> toModelSupports(
            String responseBody,
            Set<ProtocolType> upstreamProtocols,
            RoutePriority defaultPriority
    ) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            throw new BusinessException("PROVIDER_MODELS_RESPONSE_INVALID");
        }
        Set<ProtocolType> uniqueProtocols = new LinkedHashSet<>(upstreamProtocols);
        List<ChannelModelSupport> supports = new ArrayList<>();
        long idBase = Instant.now(clock).toEpochMilli() * 1_000L;
        long index = 1L;
        for (JsonNode item : data) {
            String id = item.path("id").asText("").trim();
            if (id.isBlank()) {
                continue;
            }
            ModelName modelName = ModelName.of(id);
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
        if (supports.isEmpty()) {
            throw new BusinessException("PROVIDER_MODELS_EMPTY");
        }
        return supports;
    }
}
