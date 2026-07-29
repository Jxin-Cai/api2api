package com.api2api.ohs.http.gateway;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Shared header filtering logic for gateway responses (non-streaming and streaming).
 */
final class GatewayResponseHeaderFilter {

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

    private GatewayResponseHeaderFilter() {
    }

    static boolean shouldForward(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return !FILTERED_RESPONSE_HEADERS.contains(normalized)
                && !normalized.equals(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.ROOT));
    }

    static MediaType extractContentType(Map<String, List<String>> headers, MediaType defaultType) {
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)
                        && entry.getValue() != null
                        && !entry.getValue().isEmpty()
                        && entry.getValue().get(0) != null
                        && !entry.getValue().get(0).isBlank()) {
                    return MediaType.parseMediaType(entry.getValue().get(0));
                }
            }
        }
        return defaultType;
    }
}
