package com.api2api.infr.client.provider;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Builds upstream headers for provider requests.
 *
 * <p>Inbound headers are forwarded unless explicitly denied, preserving the client fingerprint the
 * provider would observe on a direct call. The gateway only overrides what it owns: the provider
 * credential and the content negotiation it needs to read the upstream body.</p>
 */
@Component
@RequiredArgsConstructor
public class UpstreamHttpHeaderPolicy {

    private static final String ANTHROPIC_VERSION = "anthropic-version";
    private static final String ANTHROPIC_BETA = "anthropic-beta";

    @NonNull
    private final ProviderHttpClientProperties properties;

    public Map<String, String> buildHeaders(
            ProtocolType protocolType,
            Map<String, List<String>> incomingHeaders,
            String bearerToken,
            boolean streaming
    ) {
        return buildHeaders(protocolType, incomingHeaders, bearerToken, streaming, MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * @param contentType the media type of the body the gateway is about to send; the gateway owns
     *                    this header because it re-encodes every upstream body itself
     */
    public Map<String, String> buildHeaders(
            ProtocolType protocolType,
            Map<String, List<String>> incomingHeaders,
            String bearerToken,
            boolean streaming,
            String contentType
    ) {
        Objects.requireNonNull(protocolType, "Protocol type must not be null");
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("Bearer token must not be blank");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type must not be blank");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        addForwardedHeaders(headers, incomingHeaders, protocolType);
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.put(HttpHeaders.CONTENT_TYPE, contentType);
        headers.put(HttpHeaders.ACCEPT, acceptFor(protocolType, streaming));
        if (protocolType == ProtocolType.CLAUDE_MESSAGES && !containsIgnoreCase(headers, ANTHROPIC_VERSION)) {
            headers.put(ANTHROPIC_VERSION, properties.getAnthropicVersion());
        }
        return headers;
    }

    private String acceptFor(ProtocolType protocolType, boolean streaming) {
        if (!streaming) {
            return MediaType.APPLICATION_JSON_VALUE;
        }
        return protocolType == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES
                ? "application/vnd.amazon.eventstream"
                : MediaType.TEXT_EVENT_STREAM_VALUE;
    }

    private void addForwardedHeaders(
            Map<String, String> target,
            Map<String, List<String>> source,
            ProtocolType protocolType
    ) {
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
            if (protocolType == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES && ANTHROPIC_BETA.equals(normalized)) {
                return;
            }
            String merged = mergeValues(values);
            if (!merged.isEmpty()) {
                target.put(name.trim(), merged);
            }
        });
    }

    /**
     * Recombines repeated field lines into a single comma-separated value as permitted for
     * list-valued headers. Dropping the extra lines would silently disable negotiated features such
     * as the beta flags a client spreads across several {@code anthropic-beta} headers.
     */
    private static String mergeValues(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }

    private static boolean containsIgnoreCase(Map<String, String> headers, String name) {
        return headers.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
