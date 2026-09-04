package com.api2api.infr.client.provider;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.MediaType;

/**
 * Encoded bytes of an upstream request together with the {@code Content-Type} describing them.
 */
record UpstreamRequestBody(byte[] content, String contentType) {

    UpstreamRequestBody {
        content = Objects.requireNonNull(content, "content must not be null").clone();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
    }

    static UpstreamRequestBody json(String body) {
        Objects.requireNonNull(body, "body must not be null");
        return new UpstreamRequestBody(body.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
