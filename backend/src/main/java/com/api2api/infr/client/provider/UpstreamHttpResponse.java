package com.api2api.infr.client.provider;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.routing.model.RouteFailureType;
import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured upstream HTTP response or classified failure.
 */
public final class UpstreamHttpResponse {

    private final ProviderChannelId providerChannelId;
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;
    private final InputStream streamingBody;
    private final Duration elapsed;
    private final ErrorSummary errorSummary;

    private UpstreamHttpResponse(
            ProviderChannelId providerChannelId,
            int statusCode,
            Map<String, List<String>> headers,
            String body,
            InputStream streamingBody,
            Duration elapsed,
            ErrorSummary errorSummary
    ) {
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.statusCode = statusCode;
        this.headers = copyHeaders(headers);
        this.body = body;
        this.streamingBody = streamingBody;
        this.elapsed = Objects.requireNonNull(elapsed, "Elapsed duration must not be null");
        this.errorSummary = errorSummary;
        if (body != null && streamingBody != null) {
            throw new IllegalArgumentException("Response cannot carry both aggregated body and streaming body");
        }
    }

    public static UpstreamHttpResponse success(
            ProviderChannelId providerChannelId,
            int statusCode,
            Map<String, List<String>> headers,
            String body,
            Duration elapsed
    ) {
        return new UpstreamHttpResponse(providerChannelId, statusCode, headers, body, null, elapsed, null);
    }

    public static UpstreamHttpResponse streamingSuccess(
            ProviderChannelId providerChannelId,
            int statusCode,
            Map<String, List<String>> headers,
            InputStream streamingBody,
            Duration elapsed
    ) {
        return new UpstreamHttpResponse(
                providerChannelId,
                statusCode,
                headers,
                null,
                Objects.requireNonNull(streamingBody, "Streaming body must not be null"),
                elapsed,
                null
        );
    }

    public static UpstreamHttpResponse failure(
            ProviderChannelId providerChannelId,
            int statusCode,
            Map<String, List<String>> headers,
            String body,
            Duration elapsed,
            ErrorSummary errorSummary
    ) {
        return new UpstreamHttpResponse(
                providerChannelId,
                statusCode,
                headers,
                body,
                null,
                elapsed,
                Objects.requireNonNull(errorSummary, "Error summary must not be null")
        );
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public int statusCode() {
        return statusCode;
    }

    public Map<String, List<String>> headers() {
        return copyHeaders(headers);
    }

    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    public Optional<InputStream> streamingBody() {
        return Optional.ofNullable(streamingBody);
    }

    public Duration elapsed() {
        return elapsed;
    }

    public Optional<ErrorSummary> errorSummary() {
        return Optional.ofNullable(errorSummary);
    }

    public boolean successful() {
        return errorSummary == null && statusCode >= 200 && statusCode < 300;
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> source) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        if (source == null) {
            return copied;
        }
        source.forEach((name, values) -> {
            if (name != null && values != null) {
                copied.put(name, List.copyOf(values));
            }
        });
        return Map.copyOf(copied);
    }

    public record ErrorSummary(
            ProviderChannelId providerChannelId,
            Integer statusCode,
            RouteFailureType failureType,
            boolean retryable,
            String message,
            long elapsedMillis
    ) {
        public ErrorSummary {
            Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
            Objects.requireNonNull(failureType, "Failure type must not be null");
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Error message must not be blank");
            }
            message = truncate(message.trim(), 1000);
            if (elapsedMillis < 0) {
                throw new IllegalArgumentException("Elapsed millis must not be negative");
            }
        }

        private static String truncate(String value, int maxLength) {
            if (value.length() <= maxLength) {
                return value;
            }
            return value.substring(0, maxLength);
        }
    }
}
