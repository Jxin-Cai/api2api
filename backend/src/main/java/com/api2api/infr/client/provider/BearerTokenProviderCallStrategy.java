package com.api2api.infr.client.provider;

import com.api2api.application.BusinessException;
import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.application.gateway.UpstreamGatewayException;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.model.RouteFailureType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class BearerTokenProviderCallStrategy implements ProviderCallStrategy {

    private final ProviderChannelRepository providerChannelRepository;
    private final ProviderSecretResolver providerSecretResolver;
    private final ProviderHttpClientProperties properties;
    private final UpstreamHttpHeaderPolicy headerPolicy;
    private final UpstreamUrlResolver urlResolver;
    private final HttpClient httpClient;

    BearerTokenProviderCallStrategy(
            ProviderChannelRepository providerChannelRepository,
            ProviderSecretResolver providerSecretResolver,
            ProviderHttpClientProperties properties,
            UpstreamHttpHeaderPolicy headerPolicy,
            UpstreamUrlResolver urlResolver
    ) {
        this.providerChannelRepository = providerChannelRepository;
        this.providerSecretResolver = providerSecretResolver;
        this.properties = properties;
        this.headerPolicy = headerPolicy;
        this.urlResolver = urlResolver;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public boolean supports(ProtocolType upstreamProtocol) {
        return true;
    }

    @Override
    public ProviderGatewayResponse forward(
            RouteCandidate candidate,
            String upstreamRequestBody,
            boolean streaming,
            Map<String, List<String>> incomingHeaders
    ) {
        HttpRequest request = buildRequest(candidate, upstreamRequestBody, streaming, incomingHeaders);
        Instant startedAt = Instant.now();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ProviderGatewayResponse.of(
                    candidate.upstreamProtocol(),
                    response.statusCode(),
                    response.headers().map(),
                    response.body(),
                    streaming
            );
        } catch (HttpTimeoutException e) {
            throw new UpstreamGatewayException(RouteFailureType.TIMEOUT, null, true, elapsedSince(startedAt), "Upstream request timed out");
        } catch (IOException e) {
            throw new UpstreamGatewayException(RouteFailureType.CHANNEL_UNAVAILABLE, null, true, elapsedSince(startedAt), "Upstream connection failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamGatewayException(RouteFailureType.UPSTREAM_ERROR, null, false, elapsedSince(startedAt), "Upstream call interrupted");
        }
    }

    @Override
    public ProviderStreamingResponse openStream(
            RouteCandidate candidate,
            String upstreamRequestBody,
            Map<String, List<String>> incomingHeaders
    ) {
        int attempt = 0;
        while (true) {
            try {
                return openStreamOnce(candidate, upstreamRequestBody, incomingHeaders);
            } catch (UpstreamGatewayException failure) {
                if (shouldRetryStream(attempt, failure)) {
                    waitBeforeStreamRetry(candidate, attempt, failure);
                    attempt++;
                    continue;
                }
                throw failure;
            }
        }
    }

    private ProviderStreamingResponse openStreamOnce(
            RouteCandidate candidate,
            String upstreamRequestBody,
            Map<String, List<String>> incomingHeaders
    ) {
        HttpRequest request = buildRequest(candidate, upstreamRequestBody, true, incomingHeaders);
        Instant startedAt = Instant.now();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                String errorBody = readErrorBody(response.body());
                closeQuietly(response.body());
                throw toStatusFailure(statusCode, elapsedSince(startedAt), errorBody);
            }
            return ProviderStreamingResponse.of(
                    candidate.upstreamProtocol(),
                    statusCode,
                    response.headers().map(),
                    withStreamingTimeout(response.body())
            );
        } catch (HttpTimeoutException exception) {
            throw new UpstreamGatewayException(
                    RouteFailureType.TIMEOUT,
                    null,
                    true,
                    elapsedSince(startedAt),
                    "Upstream streaming response headers timed out"
            );
        } catch (IOException exception) {
            throw new UpstreamGatewayException(
                    RouteFailureType.CHANNEL_UNAVAILABLE,
                    null,
                    true,
                    elapsedSince(startedAt),
                    "Upstream streaming connection failed"
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamGatewayException(
                    RouteFailureType.UPSTREAM_ERROR,
                    null,
                    false,
                    elapsedSince(startedAt),
                    "Upstream streaming call interrupted"
            );
        }
    }

    private boolean shouldRetryStream(int attempt, UpstreamGatewayException failure) {
        return failure.retryable()
                && failure.failureType() != RouteFailureType.UPSTREAM_ERROR
                && failure.failureType() != RouteFailureType.RATE_LIMITED
                && attempt < properties.getStreamingMaxRetries();
    }

    private void waitBeforeStreamRetry(
            RouteCandidate candidate,
            int attempt,
            UpstreamGatewayException failure
    ) {
        long multiplier = 1L << Math.min(attempt, 10);
        long delayMillis = Math.multiplyExact(properties.getStreamingRetryBackoff().toMillis(), multiplier);
        log.warn(
                "Retrying upstream stream before response body, channelId: {}, upstreamProtocol: {}, "
                        + "statusCode: {}, failureType: {}, retryAttempt: {}",
                candidate.providerChannelId().value(),
                candidate.upstreamProtocol(),
                failure.statusCode(),
                failure.failureType(),
                attempt + 1
        );
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamGatewayException(
                    RouteFailureType.UPSTREAM_ERROR,
                    failure.statusCode(),
                    false,
                    failure.elapsedMillis(),
                    "Upstream streaming retry interrupted"
            );
        }
    }

    private HttpRequest buildRequest(
            RouteCandidate candidate,
            String upstreamRequestBody,
            boolean streaming,
            Map<String, List<String>> incomingHeaders
    ) {
        ProviderChannel channel = providerChannelRepository.findById(candidate.providerChannelId())
                .orElseThrow(() -> new BusinessException("PROVIDER_CHANNEL_NOT_FOUND"));
        if (!channel.isEnabledForRouting()) {
            throw new UpstreamGatewayException(
                    RouteFailureType.CHANNEL_UNAVAILABLE,
                    null,
                    true,
                    0,
                    "Provider channel is not enabled for routing"
            );
        }
        if (!channel.supportsModel(candidate.requestedModel(), candidate.upstreamProtocol())) {
            throw new UpstreamGatewayException(
                    RouteFailureType.CHANNEL_UNAVAILABLE,
                    null,
                    true,
                    0,
                    "Model is not enabled for routing"
            );
        }
        String secret = providerSecretResolver.resolve(channel.keyRef());
        String path = resolveUpstreamPath(candidate, streaming);
        URI uri = OutboundUriGuard.verify(
                URI.create(urlResolver.resolve(channel.host().resolvePath(path).value())),
                properties.isAllowInsecureHosts()
        );
        Map<String, String> headers = headerPolicy.buildHeaders(
                candidate.upstreamProtocol(), incomingHeaders, secret, streaming);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(readTimeout(streaming))
                .POST(HttpRequest.BodyPublishers.ofString(upstreamRequestBody));
        headers.forEach(requestBuilder::header);
        return requestBuilder.build();
    }

    private String resolveUpstreamPath(RouteCandidate candidate, boolean streaming) {
        if (candidate.upstreamProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE) {
            String template = streaming
                    ? properties.getBedrockConverseStreamPathTemplate()
                    : properties.getBedrockConversePathTemplate();
            return template.replace("{modelId}", candidate.upstreamModel().value());
        }
        if (candidate.upstreamProtocol() == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES) {
            String template = streaming
                    ? properties.getBedrockClaudeMessagesStreamPathTemplate()
                    : properties.getBedrockClaudeMessagesPathTemplate();
            return template.replace("{modelId}", candidate.upstreamModel().value());
        }
        return properties.defaultPathFor(candidate.upstreamProtocol());
    }

    private UpstreamGatewayException toStatusFailure(int statusCode, long elapsedMillis, String responseBody) {
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
            retryable = isModelUnavailable(statusCode, responseBody);
        }
        String message = "Upstream returned HTTP " + statusCode;
        if (responseBody != null && !responseBody.isBlank()) {
            String compact = responseBody.replaceAll("\\s+", " ").trim();
            if (compact.length() > 500) {
                compact = compact.substring(0, 500) + "...";
            }
            message += ": " + compact;
        }
        return new UpstreamGatewayException(failureType, statusCode, retryable, elapsedMillis, message);
    }

    private boolean isModelUnavailable(int statusCode, String responseBody) {
        if (statusCode != 404 || responseBody == null) {
            return false;
        }
        String normalized = responseBody.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("model_not_found")
                || normalized.contains("model not found")
                || normalized.contains("not supported by any configured account");
    }

    private Duration readTimeout(boolean streaming) {
        return streaming ? properties.getStreamingFirstByteTimeout() : properties.getUpstreamReadTimeout();
    }

    private InputStream withStreamingTimeout(InputStream body) {
        return new StreamingIdleTimeoutInputStream(
                body,
                properties.getStreamingFirstByteTimeout(),
                properties.getStreamingIdleTimeout()
        );
    }

    private String readErrorBody(InputStream body) {
        if (body == null) {
            return "";
        }
        try {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private void closeQuietly(InputStream body) {
        if (body == null) {
            return;
        }
        try {
            body.close();
        } catch (IOException exception) {
            log.warn("Failed to close upstream response body", exception);
        }
    }

    private long elapsedSince(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

}
