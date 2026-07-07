package com.api2api.application.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Streaming response opened from an upstream provider and owned by the gateway response writer.
 */
public final class ProviderStreamingResponse implements AutoCloseable {

    private final InputStream body;

    private ProviderStreamingResponse(InputStream body) {
        this.body = Objects.requireNonNull(body, "Streaming response body must not be null");
    }

    public static ProviderStreamingResponse of(InputStream body) {
        return new ProviderStreamingResponse(body);
    }

    public InputStream body() {
        return body;
    }

    @Override
    public void close() throws IOException {
        body.close();
    }
}
