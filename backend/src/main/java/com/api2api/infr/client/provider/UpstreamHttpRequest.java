package com.api2api.infr.client.provider;

import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.routing.model.RouteCandidate;
import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Infrastructure request model for forwarding an already converted protocol payload upstream.
 */
public final class UpstreamHttpRequest {

    private final ProviderChannel channel;
    private final RouteCandidate candidate;
    private final String method;
    private final String path;
    private final Map<String, List<String>> headers;
    private final String body;
    private final boolean streaming;

    private UpstreamHttpRequest(Builder builder) {
        this.channel = Objects.requireNonNull(builder.channel, "Provider channel must not be null");
        this.candidate = Objects.requireNonNull(builder.candidate, "Route candidate must not be null");
        this.method = normalizeMethod(builder.method);
        this.path = normalizePath(builder.path);
        this.headers = copyHeaders(builder.headers);
        this.body = builder.body == null ? "" : builder.body;
        this.streaming = builder.streaming;
        if (!channel.id().equals(candidate.providerChannelId())) {
            throw new IllegalArgumentException("Request channel id must match route candidate provider channel id");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProviderChannel channel() {
        return channel;
    }

    public RouteCandidate candidate() {
        return candidate;
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Map<String, List<String>> headers() {
        return copyHeaders(headers);
    }

    public String body() {
        return body;
    }

    public boolean streaming() {
        return streaming;
    }

    HttpRequest.BodyPublisher bodyPublisher() {
        if (body.isEmpty() && ("GET".equals(method) || "DELETE".equals(method))) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofString(body);
    }

    private static String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "POST";
        }
        return method.trim().toUpperCase();
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) {
            throw new IllegalArgumentException("Upstream path must start with /");
        }
        return trimmed;
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> source) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        if (source == null) {
            return copied;
        }
        source.forEach((name, values) -> {
            if (name != null && !name.isBlank() && values != null) {
                copied.put(name.trim(), List.copyOf(values));
            }
        });
        return Map.copyOf(copied);
    }

    public static final class Builder {
        private ProviderChannel channel;
        private RouteCandidate candidate;
        private String method = "POST";
        private String path;
        private Map<String, List<String>> headers = new LinkedHashMap<>();
        private String body;
        private boolean streaming;

        private Builder() {
        }

        public Builder channel(ProviderChannel channel) {
            this.channel = channel;
            return this;
        }

        public Builder candidate(RouteCandidate candidate) {
            this.candidate = candidate;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public UpstreamHttpRequest build() {
            return new UpstreamHttpRequest(this);
        }
    }
}
