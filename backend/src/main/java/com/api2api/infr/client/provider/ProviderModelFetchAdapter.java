package com.api2api.infr.client.provider;

import com.api2api.application.channel.ProviderModelFetchPort;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.RoutePriority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
    private final Clock clock;

    private final HttpClient httpClient;

    public ProviderModelFetchAdapter(
            ProviderSecretResolver providerSecretResolver,
            ProviderHttpClientProperties properties,
            UpstreamHttpHeaderPolicy headerPolicy,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.providerSecretResolver = Objects.requireNonNull(providerSecretResolver, "Provider secret resolver must not be null");
        this.properties = Objects.requireNonNull(properties, "Provider HTTP client properties must not be null");
        this.headerPolicy = Objects.requireNonNull(headerPolicy, "Upstream HTTP header policy must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    @Override
    public List<ChannelModelSupport> fetchModels(
            ProviderChannelId channelId,
            ProviderHost host,
            ProviderKeyRef keyRef,
            Set<ProtocolType> supportedProtocols,
            RoutePriority defaultPriority
    ) {
        Objects.requireNonNull(channelId, "Provider channel id must not be null");
        Objects.requireNonNull(host, "Provider host must not be null");
        Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        Objects.requireNonNull(supportedProtocols, "Supported protocols must not be null");
        Objects.requireNonNull(defaultPriority, "Default route priority must not be null");

        String secret = providerSecretResolver.resolve(keyRef);
        URI uri = URI.create(host.resolvePath(properties.getModelsPath()).value());
        Map<String, String> headers = headerPolicy.buildHeaders(ProtocolType.OPENAI_CHAT_COMPLETIONS, Map.of(), secret, false);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(properties.getModelsReadTimeout())
                .GET();
        headers.forEach(requestBuilder::header);

        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("PROVIDER_MODELS_HTTP_" + response.statusCode());
            }
            return toModelSupports(response.body(), supportedProtocols, defaultPriority);
        } catch (IOException exception) {
            throw new IllegalStateException("PROVIDER_MODELS_IO_ERROR: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PROVIDER_MODELS_INTERRUPTED", exception);
        }
    }

    private List<ChannelModelSupport> toModelSupports(
            String responseBody,
            Set<ProtocolType> supportedProtocols,
            RoutePriority defaultPriority
    ) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            throw new IllegalStateException("PROVIDER_MODELS_RESPONSE_INVALID");
        }
        List<ChannelModelSupport> supports = new ArrayList<>();
        long idBase = Instant.now(clock).toEpochMilli() * 1_000L;
        long index = 1L;
        for (JsonNode item : data) {
            String id = item.path("id").asText("").trim();
            if (id.isBlank()) {
                continue;
            }
            ModelName modelName = ModelName.of(id);
            for (ProtocolType protocol : supportedProtocols) {
                supports.add(ChannelModelSupport.create(
                        ChannelModelSupportId.of(idBase + index++),
                        modelName,
                        modelName,
                        protocol,
                        defaultPriority,
                        ModelSupportSource.FETCHED,
                        Instant.now(clock)
                ));
            }
        }
        if (supports.isEmpty()) {
            throw new IllegalStateException("PROVIDER_MODELS_EMPTY");
        }
        return supports;
    }
}
