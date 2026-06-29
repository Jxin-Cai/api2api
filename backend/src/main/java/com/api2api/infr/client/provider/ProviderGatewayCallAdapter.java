package com.api2api.infr.client.provider;

import com.api2api.application.BusinessException;
import com.api2api.application.gateway.ProviderGatewayCallPort;
import com.api2api.application.gateway.UpstreamGatewayException;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.protocol.model.ConversionPayload;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.model.RouteFailureType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import org.springframework.stereotype.Component;

/**
 * HTTP adapter that forwards converted gateway payloads to the selected upstream provider.
 */
@Component
public class ProviderGatewayCallAdapter implements ProviderGatewayCallPort {

    @NonNull
    private final ProviderChannelRepository providerChannelRepository;

    @NonNull
    private final ProviderSecretResolver providerSecretResolver;

    @NonNull
    private final ProviderHttpClientProperties properties;

    @NonNull
    private final UpstreamHttpHeaderPolicy headerPolicy;

    private final HttpClient httpClient;

    public ProviderGatewayCallAdapter(
            ProviderChannelRepository providerChannelRepository,
            ProviderSecretResolver providerSecretResolver,
            ProviderHttpClientProperties properties,
            UpstreamHttpHeaderPolicy headerPolicy
    ) {
        this.providerChannelRepository = Objects.requireNonNull(providerChannelRepository, "Provider channel repository must not be null");
        this.providerSecretResolver = Objects.requireNonNull(providerSecretResolver, "Provider secret resolver must not be null");
        this.properties = Objects.requireNonNull(properties, "Provider HTTP client properties must not be null");
        this.headerPolicy = Objects.requireNonNull(headerPolicy, "Upstream HTTP header policy must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    @Override
    public ConversionPayload forward(RouteCandidate candidate, String upstreamRequestBody, boolean streaming) {
        Objects.requireNonNull(candidate, "Route candidate must not be null");
        Objects.requireNonNull(upstreamRequestBody, "Upstream request body must not be null");

        ProviderChannel channel = providerChannelRepository.findById(candidate.providerChannelId())
                .orElseThrow(() -> new BusinessException("PROVIDER_CHANNEL_NOT_FOUND"));
        String secret = providerSecretResolver.resolve(channel.keyRef());
        String path = properties.defaultPathFor(candidate.upstreamProtocol());
        URI uri = OutboundUriGuard.verify(
                URI.create(channel.host().resolvePath(path).value()),
                properties.isAllowInsecureHosts()
        );
        Map<String, String> headers = headerPolicy.buildHeaders(candidate.upstreamProtocol(), Map.of(), secret, streaming);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(readTimeout(streaming))
                .POST(HttpRequest.BodyPublishers.ofString(upstreamRequestBody));
        headers.forEach(requestBuilder::header);

        Instant startedAt = Instant.now();
        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw toStatusFailure(statusCode, elapsedMillis);
            }
            return ConversionPayload.of(candidate.upstreamProtocol(), response.body(), streaming);
        } catch (HttpTimeoutException exception) {
            throw new UpstreamGatewayException(RouteFailureType.TIMEOUT, null, true, elapsedSince(startedAt), "Upstream request timed out");
        } catch (IOException exception) {
            throw new UpstreamGatewayException(RouteFailureType.CHANNEL_UNAVAILABLE, null, true, elapsedSince(startedAt), "Upstream connection failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamGatewayException(RouteFailureType.UPSTREAM_ERROR, null, false, elapsedSince(startedAt), "Upstream call interrupted");
        }
    }

    private UpstreamGatewayException toStatusFailure(int statusCode, long elapsedMillis) {
        RouteFailureType failureType;
        boolean retryable;
        if (statusCode == 401 || statusCode == 403) {
            failureType = RouteFailureType.AUTHORIZATION_ERROR;
            retryable = false;
        } else if (statusCode == 429) {
            failureType = RouteFailureType.RATE_LIMITED;
            retryable = true;
        } else if (statusCode >= 500) {
            failureType = RouteFailureType.CHANNEL_UNAVAILABLE;
            retryable = true;
        } else {
            failureType = RouteFailureType.UPSTREAM_ERROR;
            retryable = false;
        }
        return new UpstreamGatewayException(failureType, statusCode, retryable, elapsedMillis, "Upstream returned HTTP " + statusCode);
    }

    private Duration readTimeout(boolean streaming) {
        return streaming ? properties.getStreamingFirstByteTimeout() : properties.getUpstreamReadTimeout();
    }

    private long elapsedSince(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
