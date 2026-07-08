package com.api2api.application.gateway;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raw HTTP response returned by an upstream provider for a gateway call.
 */
public final class ProviderGatewayResponse {

    private final ProtocolType protocol;
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;
    private final boolean streaming;

    private ProviderGatewayResponse(
            ProtocolType protocol,
            int statusCode,
            Map<String, List<String>> headers,
            String body,
            boolean streaming
    ) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("HTTP status code must be between 100 and 599");
        }
        this.protocol = Objects.requireNonNull(protocol, "Response protocol must not be null");
        this.statusCode = statusCode;
        this.headers = copyHeaders(headers);
        this.body = Objects.requireNonNull(body, "Response body must not be null");
        this.streaming = streaming;
    }

    public static ProviderGatewayResponse of(
            ProtocolType protocol,
            int statusCode,
            Map<String, List<String>> headers,
            String body,
            boolean streaming
    ) {
        return new ProviderGatewayResponse(protocol, statusCode, headers, body, streaming);
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().trim(),
                        entry -> List.copyOf(entry.getValue()),
                        (left, right) -> left
                ));
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public ProtocolType protocol() {
        return protocol;
    }

    public int statusCode() {
        return statusCode;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public String body() {
        return body;
    }

    public boolean streaming() {
        return streaming;
    }
}
