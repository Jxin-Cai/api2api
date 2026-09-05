package com.api2api.infr.client.provider;

import com.api2api.application.BusinessException;
import com.api2api.application.gateway.InboundRequestContext;
import com.api2api.application.gateway.MultipartFormPayloadCodec;
import com.api2api.application.gateway.ProtocolOperation;
import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.application.gateway.UpstreamGatewayException;
import com.api2api.application.gateway.UpstreamResponseMetadata;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class BearerTokenProviderCallStrategy implements ProviderCallStrategy {

    private final ProviderChannelRepository providerChannelRepository;
    private final ProviderHttpClientProperties properties;
    private final UpstreamHttpHeaderPolicy headerPolicy;
    private final UpstreamUrlResolver urlResolver;
    private final MultipartFormPayloadCodec multipartFormCodec;
    private final HttpClient httpClient;

    BearerTokenProviderCallStrategy(
            ProviderChannelRepository providerChannelRepository,
            ProviderHttpClientProperties properties,
            UpstreamHttpHeaderPolicy headerPolicy,
            UpstreamUrlResolver urlResolver,
            MultipartFormPayloadCodec multipartFormCodec
    ) {
        this.providerChannelRepository = providerChannelRepository;
        this.properties = properties;
        this.headerPolicy = headerPolicy;
        this.urlResolver = urlResolver;
        this.multipartFormCodec = multipartFormCodec;
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
            InboundRequestContext inbound
    ) {
        HttpRequest request = buildRequest(candidate, upstreamRequestBody, streaming, inbound);
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
            InboundRequestContext inbound
    ) {
        int attempt = 0;
        while (true) {
            try {
                return openStreamOnce(candidate, upstreamRequestBody, inbound);
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
            InboundRequestContext inbound
    ) {
        HttpRequest request = buildRequest(candidate, upstreamRequestBody, true, inbound);
        Instant startedAt = Instant.now();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                String errorBody = readErrorBody(response.body());
                closeQuietly(response.body());
                throw toStatusFailure(
                        statusCode,
                        elapsedSince(startedAt),
                        errorBody,
                        UpstreamResponseMetadata.of(response.headers().map())
                );
            }
            return ProviderStreamingResponse.of(
                    candidate.upstreamProtocol(),
                    statusCode,
                    response.headers().map(),
                    prepareStreamingBody(candidate, response, startedAt)
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

    /**
     * Preserve the existing transport retry policy and additionally retry explicit capacity
     * rejections detected before output. A timeout or arbitrary server error is not replay-safe.
     */
    private boolean shouldRetryStream(int attempt, UpstreamGatewayException failure) {
        boolean retryableTransport = failure.failureType() == RouteFailureType.CHANNEL_UNAVAILABLE
                && failure.statusCode() == null;
        boolean explicitOverload = failure instanceof UpstreamStreamOverloadedException;
        return failure.retryable()
                && attempt < properties.getStreamingMaxRetries()
                && (retryableTransport || explicitOverload)
                // Do not sleep for an unbounded Retry-After or retry earlier than the provider asks.
                // Leave such failures to the route failover policy, preserving response metadata.
                && (!explicitOverload || failure.responseMetadata().retryAfter(Instant.now()).isEmpty());
    }

    private InputStream prepareStreamingBody(
            RouteCandidate candidate,
            HttpResponse<InputStream> response,
            Instant startedAt
    ) {
        InputStream body = withStreamingTimeout(response.body());
        if (candidate.upstreamProtocol() != ProtocolType.OPENAI_RESPONSES
                || !response.headers().firstValue("Content-Type").orElse("")
                        .split(";", 2)[0].trim().equalsIgnoreCase("text/event-stream")) {
            return body;
        }
        boolean handedOff = false;
        try {
            InputStream inspected = ResponsesStreamPreflight.inspect(
                    body, startedAt, UpstreamResponseMetadata.of(response.headers().map()));
            handedOff = true;
            return inspected;
        } catch (IOException exception) {
            // HTTP headers were received: do not misclassify a body read failure as a connection
            // that never reached the provider and replay potentially running generation.
            UpstreamGatewayException failure = new UpstreamGatewayException(
                    exception instanceof java.net.SocketTimeoutException
                            ? RouteFailureType.TIMEOUT : RouteFailureType.CHANNEL_UNAVAILABLE,
                    response.statusCode(), false, elapsedSince(startedAt),
                    "Upstream stream failed during preflight", UpstreamResponseMetadata.of(response.headers().map()));
            failure.initCause(exception);
            throw failure;
        } finally {
            if (!handedOff) {
                closeQuietly(body);
            }
        }
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
            InboundRequestContext inbound
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
        String secret = channel.keyRef().value();
        String path = resolveUpstreamPath(candidate, streaming, inbound.operation());
        String baseUrl = urlResolver.resolve(channel.host().resolvePath(path).value());
        String query = candidate.requiresProtocolConversion() ? null : inbound.rawQuery();
        URI uri = OutboundUriGuard.verify(
                URI.create(appendQuery(baseUrl, query)),
                properties.isAllowInsecureHosts()
        );
        UpstreamRequestBody body = encodeBody(upstreamRequestBody, inbound.operation());
        var headers = headerPolicy.buildHeaders(
                candidate.upstreamProtocol(), inbound.headers(), secret, streaming, body.contentType());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(readTimeout(streaming))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.content()));
        headers.forEach(requestBuilder::setHeader);
        return requestBuilder.build();
    }

    /**
     * The pipeline hands over JSON text for every operation; multipart operations carry their form as
     * a JSON envelope that is re-encoded here so the provider receives real {@code multipart/form-data}.
     */
    private UpstreamRequestBody encodeBody(String upstreamRequestBody, ProtocolOperation operation) {
        if (!operation.acceptsMultipartForm()) {
            return UpstreamRequestBody.json(upstreamRequestBody);
        }
        return MultipartFormBodyWriter.write(multipartFormCodec.decode(upstreamRequestBody));
    }

    /**
     * Carries the inbound query string through untouched; providers gate optional behaviour on it
     * (for example Anthropic's {@code ?beta=true}), so dropping it silently changes the response.
     */
    private static String appendQuery(String baseUrl, String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return baseUrl;
        }
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + rawQuery;
    }

    private String resolveUpstreamPath(RouteCandidate candidate, boolean streaming, ProtocolOperation operation) {
        ProtocolType upstreamProtocol = candidate.upstreamProtocol();
        if (!operation.availableOn(upstreamProtocol)) {
            throw new UpstreamGatewayException(
                    RouteFailureType.CHANNEL_UNAVAILABLE,
                    null,
                    true,
                    0,
                    "Operation " + operation + " is not available on " + upstreamProtocol + " routes"
            );
        }
        if (upstreamProtocol == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES) {
            String template = streaming
                    ? properties.getBedrockClaudeMessagesStreamPathTemplate()
                    : properties.getBedrockClaudeMessagesPathTemplate();
            return template.replace("{modelId}", candidate.upstreamModel().value());
        }
        return properties.upstreamPathFor(upstreamProtocol, operation);
    }

    private UpstreamGatewayException toStatusFailure(
            int statusCode,
            long elapsedMillis,
            String responseBody,
            UpstreamResponseMetadata responseMetadata
    ) {
        RouteFailureType failureType = UpstreamFailureClassifier.fromHttpStatus(statusCode);
        boolean retryable;
        if (failureType == RouteFailureType.AUTHORIZATION_ERROR) {
            retryable = false;
        } else if (failureType == RouteFailureType.UPSTREAM_ERROR) {
            retryable = UpstreamFailureClassifier.isModelUnavailable(statusCode, responseBody);
        } else {
            retryable = true;
        }
        String message = UpstreamFailureClassifier.compactErrorMessage("Upstream", statusCode, responseBody);
        return new UpstreamGatewayException(failureType, statusCode, retryable, elapsedMillis, message, responseMetadata);
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
        } catch (IOException exception) {
            log.warn("Failed to read upstream error body", exception);
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
