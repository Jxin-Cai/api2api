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
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

@Component
class BedrockProviderCallStrategy implements ProviderCallStrategy {

    private final ProviderChannelRepository providerChannelRepository;
    private final BedrockCredentialResolver credentialResolver;
    private final ProviderHttpClientProperties properties;
    private final HttpClient httpClient;

    BedrockProviderCallStrategy(
            ProviderChannelRepository providerChannelRepository,
            BedrockCredentialResolver credentialResolver,
            ProviderHttpClientProperties properties
    ) {
        this.providerChannelRepository = providerChannelRepository;
        this.credentialResolver = credentialResolver;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public boolean supports(ProtocolType upstreamProtocol) {
        // SigV4 direct-to-AWS strategy is currently disabled in favor of enterprise proxy (Bearer Token).
        // Re-enable when direct AWS Bedrock access without a proxy is needed.
        return false;
    }

    @Override
    public ProviderGatewayResponse forward(
            RouteCandidate candidate,
            String upstreamRequestBody,
            boolean streaming,
            Map<String, List<String>> incomingHeaders
    ) {
        ProviderChannel channel = loadChannel(candidate);
        BedrockCredentials credentials = credentialResolver.resolve(channel.host(), channel.keyRef());
        URI uri = buildBedrockUri(credentials.region(), candidate.upstreamModel().value(), streaming);
        HttpRequest request = buildSignedRequest(uri, upstreamRequestBody, credentials);

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
            throw new UpstreamGatewayException(RouteFailureType.TIMEOUT, null, true, elapsedSince(startedAt), "Bedrock request timed out");
        } catch (IOException e) {
            throw new UpstreamGatewayException(RouteFailureType.CHANNEL_UNAVAILABLE, null, true, elapsedSince(startedAt), "Bedrock connection failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamGatewayException(RouteFailureType.UPSTREAM_ERROR, null, false, elapsedSince(startedAt), "Bedrock call interrupted");
        }
    }

    @Override
    public ProviderStreamingResponse openStream(
            RouteCandidate candidate,
            String upstreamRequestBody,
            Map<String, List<String>> incomingHeaders
    ) {
        ProviderChannel channel = loadChannel(candidate);
        BedrockCredentials credentials = credentialResolver.resolve(channel.host(), channel.keyRef());
        URI uri = buildBedrockUri(credentials.region(), candidate.upstreamModel().value(), true);
        HttpRequest request = buildSignedRequest(uri, upstreamRequestBody, credentials);

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
                    new StreamingIdleTimeoutInputStream(
                            response.body(),
                            properties.getStreamingFirstByteTimeout(),
                            properties.getStreamingIdleTimeout()
                    )
            );
        } catch (HttpTimeoutException e) {
            throw new UpstreamGatewayException(RouteFailureType.TIMEOUT, null, true, elapsedSince(startedAt), "Bedrock streaming timed out");
        } catch (IOException e) {
            throw new UpstreamGatewayException(RouteFailureType.CHANNEL_UNAVAILABLE, null, true, elapsedSince(startedAt), "Bedrock streaming connection failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamGatewayException(RouteFailureType.UPSTREAM_ERROR, null, false, elapsedSince(startedAt), "Bedrock streaming interrupted");
        }
    }

    private URI buildBedrockUri(String region, String modelId, boolean streaming) {
        String path = streaming
                ? "/model/" + modelId + "/converse-stream"
                : "/model/" + modelId + "/converse";
        return URI.create("https://bedrock-runtime." + region + ".amazonaws.com" + path);
    }

    private HttpRequest buildSignedRequest(URI uri, String body, BedrockCredentials credentials) {
        SdkHttpFullRequest.Builder sdkRequestBuilder = SdkHttpFullRequest.builder()
                .uri(uri)
                .method(SdkHttpMethod.POST)
                .putHeader("Content-Type", "application/json")
                .contentStreamProvider(() -> new java.io.ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(buildAwsCredentials(credentials))
                .signingName("bedrock")
                .signingRegion(Region.of(credentials.region()))
                .build();

        SdkHttpFullRequest signedRequest = Aws4Signer.create().sign(sdkRequestBuilder.build(), signerParams);

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder(uri)
                .timeout(properties.getUpstreamReadTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(body));

        signedRequest.headers().forEach((name, values) -> {
            for (String value : values) {
                httpRequestBuilder.header(name, value);
            }
        });

        return httpRequestBuilder.build();
    }

    private software.amazon.awssdk.auth.credentials.AwsCredentials buildAwsCredentials(BedrockCredentials credentials) {
        if (credentials.sessionToken() != null) {
            return AwsSessionCredentials.create(
                    credentials.accessKeyId(),
                    credentials.secretAccessKey(),
                    credentials.sessionToken()
            );
        }
        return AwsBasicCredentials.create(credentials.accessKeyId(), credentials.secretAccessKey());
    }

    private ProviderChannel loadChannel(RouteCandidate candidate) {
        return providerChannelRepository.findById(candidate.providerChannelId())
                .orElseThrow(() -> new BusinessException("PROVIDER_CHANNEL_NOT_FOUND"));
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
            retryable = false;
        }
        String message = "Bedrock returned HTTP " + statusCode;
        if (responseBody != null && !responseBody.isBlank()) {
            String compact = responseBody.replaceAll("\\s+", " ").trim();
            if (compact.length() > 500) {
                compact = compact.substring(0, 500) + "...";
            }
            message += ": " + compact;
        }
        return new UpstreamGatewayException(failureType, statusCode, retryable, elapsedMillis, message);
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
        } catch (IOException ignored) {
        }
    }

    private long elapsedSince(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
