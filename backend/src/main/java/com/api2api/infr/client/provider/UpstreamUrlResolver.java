package com.api2api.infr.client.provider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Resolves upstream URLs before outbound calls.
 *
 * <p>When the backend runs in Docker, a provider host entered as localhost or 127.0.0.1
 * points to the backend container itself. Configure an override such as host.docker.internal
 * to reach services running on the Docker host.</p>
 */
@Component
public class UpstreamUrlResolver {

    private final ProviderHttpClientProperties properties;

    public UpstreamUrlResolver(ProviderHttpClientProperties properties) {
        this.properties = Objects.requireNonNull(properties, "Provider HTTP client properties must not be null");
    }

    public String resolve(String url) {
        String override = properties.getUpstreamHostOverride();
        if (url == null || override == null || override.isBlank()) {
            return url;
        }
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (!isLocalhost(host)) {
            return url;
        }
        try {
            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    override.trim(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Upstream URL is invalid", exception);
        }
    }

    private boolean isLocalhost(String host) {
        return host != null && (host.equalsIgnoreCase("localhost") || "127.0.0.1".equals(host));
    }
}
