package com.api2api.ohs.http.gateway;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Raw protocol response returned to external SDKs without management API wrapping.
 */
public final class GatewayRawResponse {

    private static final Set<String> FILTERED_RESPONSE_HEADERS = Set.of(
            "connection",
            "content-length",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "set-cookie",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    private final String body;
    private final int statusCode;
    private final MediaType contentType;
    private final Map<String, List<String>> headers;

    private GatewayRawResponse(String body, int statusCode, MediaType contentType, Map<String, List<String>> headers) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("HTTP status code must be between 100 and 599");
        }
        this.body = Objects.requireNonNull(body, "Response body must not be null");
        this.statusCode = statusCode;
        this.contentType = Objects.requireNonNull(contentType, "Content type must not be null");
        this.headers = copyHeaders(headers);
    }

    public static GatewayRawResponse of(String body, int statusCode, MediaType contentType) {
        return new GatewayRawResponse(body, statusCode, contentType, Map.of());
    }

    public static GatewayRawResponse of(
            String body,
            int statusCode,
            MediaType contentType,
            Map<String, List<String>> headers
    ) {
        return new GatewayRawResponse(body, statusCode, contentType, headers);
    }

    public ResponseEntity<String> toResponseEntity() {
        HttpHeaders responseHeaders = new HttpHeaders();
        headers.forEach((name, values) -> {
            if (shouldForwardHeader(name)) {
                responseHeaders.put(name, List.copyOf(values));
            }
        });
        responseHeaders.setContentType(contentType);
        return ResponseEntity.status(statusCode)
                .headers(responseHeaders)
                .body(body);
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

    private static boolean shouldForwardHeader(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return !FILTERED_RESPONSE_HEADERS.contains(normalized)
                && !normalized.equals(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.ROOT));
    }

    public String body() {
        return body;
    }

    public int statusCode() {
        return statusCode;
    }

    public MediaType contentType() {
        return contentType;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }
}
