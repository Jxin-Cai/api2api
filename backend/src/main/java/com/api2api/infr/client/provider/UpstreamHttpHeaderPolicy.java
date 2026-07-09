package com.api2api.infr.client.provider;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Builds sanitized upstream headers for provider requests.
 */
@Component
@RequiredArgsConstructor
public class UpstreamHttpHeaderPolicy {

    private static final String MASKED = "***";

    @NonNull
    private final ProviderHttpClientProperties properties;

    public Map<String, String> buildHeaders(
            ProtocolType protocolType,
            Map<String, List<String>> incomingHeaders,
            String bearerToken,
            boolean streaming
    ) {
        Objects.requireNonNull(protocolType, "Protocol type must not be null");
        if (protocolType == ProtocolType.AWS_BEDROCK_CONVERSE) {
            throw new IllegalArgumentException("Bedrock Converse headers are managed by BedrockProviderCallStrategy");
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("Bearer token must not be blank");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        addAllowedPassthroughHeaders(headers, incomingHeaders);
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.put(HttpHeaders.ACCEPT, streaming ? MediaType.TEXT_EVENT_STREAM_VALUE : MediaType.APPLICATION_JSON_VALUE);
        if (protocolType == ProtocolType.CLAUDE_MESSAGES) {
            headers.put("anthropic-version", properties.getAnthropicVersion());
        }
        return headers;
    }

    public Map<String, List<String>> sanitizeForDiagnostics(Map<String, List<String>> headers) {
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        if (headers == null) {
            return sanitized;
        }
        headers.forEach((name, values) -> {
            if (name == null || name.isBlank()) {
                return;
            }
            if (isSensitive(name)) {
                sanitized.put(name, List.of(MASKED));
            } else if (values != null) {
                sanitized.put(name, List.copyOf(values));
            }
        });
        return Map.copyOf(sanitized);
    }

    public String maskHeaderValue(String headerName, String headerValue) {
        if (isSensitive(headerName)) {
            return MASKED;
        }
        return headerValue;
    }

    private void addAllowedPassthroughHeaders(Map<String, String> target, Map<String, List<String>> source) {
        if (source == null) {
            return;
        }
        source.forEach((name, values) -> {
            if (name == null || name.isBlank() || values == null || values.isEmpty()) {
                return;
            }
            String normalized = normalizeName(name);
            if (properties.getHeaderDenylist().contains(normalized)) {
                return;
            }
            if (!properties.getPassthroughHeaderAllowlist().contains(normalized)) {
                return;
            }
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .ifPresent(value -> target.put(name.trim(), value));
        });
    }

    private boolean isSensitive(String headerName) {
        String normalized = normalizeName(headerName);
        return properties.getHeaderDenylist().contains(normalized)
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("key")
                || normalized.equals("authorization")
                || normalized.equals("cookie")
                || normalized.equals("set-cookie");
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
