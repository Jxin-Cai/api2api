package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UpstreamUrlResolverTest {

    @Test
    void keepsUrlWhenOverrideIsBlank() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        UpstreamUrlResolver resolver = new UpstreamUrlResolver(properties);

        assertThat(resolver.resolve("http://localhost:4141/v1/models"))
                .isEqualTo("http://localhost:4141/v1/models");
    }

    @Test
    void replacesLocalhostWithConfiguredDockerHost() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setUpstreamHostOverride("host.docker.internal");
        UpstreamUrlResolver resolver = new UpstreamUrlResolver(properties);

        assertThat(resolver.resolve("http://localhost:4141/v1/models?limit=10"))
                .isEqualTo("http://host.docker.internal:4141/v1/models?limit=10");
    }

    @Test
    void replacesLoopbackWithConfiguredDockerHost() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setUpstreamHostOverride("host.docker.internal");
        UpstreamUrlResolver resolver = new UpstreamUrlResolver(properties);

        assertThat(resolver.resolve("http://127.0.0.1:4141/v1/models"))
                .isEqualTo("http://host.docker.internal:4141/v1/models");
    }

    @Test
    void keepsNonLocalhostUrl() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setUpstreamHostOverride("host.docker.internal");
        UpstreamUrlResolver resolver = new UpstreamUrlResolver(properties);

        assertThat(resolver.resolve("https://api.example.com/v1/models"))
                .isEqualTo("https://api.example.com/v1/models");
    }
}
