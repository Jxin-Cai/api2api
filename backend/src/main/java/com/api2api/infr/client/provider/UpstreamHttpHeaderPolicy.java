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

    @NonNull
    private final ProviderHttpClientProperties properties;

    public Map<String, String> buildHeaders(
            ProtocolType protocolType,
            Map<String, List<String>> incomingHeaders,
            String bearerToken,
            boolean streaming
    ) {
        Objects.requireNonNull(protocolType, "Protocol type must not be null");
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("Bearer token must not be blank");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        addAllowedPassthroughHeaders(headers, incomingHeaders);
        if (protocolType == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES) {
            headers.entrySet().removeIf(entry -> "anthropic-beta".equalsIgnoreCase(entry.getKey()));
        }
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.put(HttpHeaders.ACCEPT, streaming && protocolType == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES
                ? "application/vnd.amazon.eventstream"
                : streaming ? MediaType.TEXT_EVENT_STREAM_VALUE : MediaType.APPLICATION_JSON_VALUE);
        if (protocolType == ProtocolType.CLAUDE_MESSAGES) {
            headers.put("anthropic-version", properties.getAnthropicVersion());
        }
        return headers;
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

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
