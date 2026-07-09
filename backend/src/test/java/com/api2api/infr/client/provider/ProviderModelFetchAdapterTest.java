package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProviderModelFetchAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesOpenAiCompatibleModelList() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("GET");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-secret");
            byte[] body = "{\"object\":\"list\",\"data\":[{\"id\":\"gpt-4.1\"},{\"id\":\"claude-sonnet\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ProviderSecretProperties secretProperties = new ProviderSecretProperties();
        secretProperties.setKeys(Map.of("test-key", "test-secret"));
        MockEnvironment mockEnv = new MockEnvironment();
        ProviderSecretResolver secretResolver = new ProviderSecretResolver(secretProperties, mockEnv);
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        ProviderModelFetchAdapter adapter = new ProviderModelFetchAdapter(
                secretResolver,
                properties,
                new UpstreamHttpHeaderPolicy(properties),
                new ObjectMapper(),
                new UpstreamUrlResolver(properties),
                new BedrockCredentialResolver(secretResolver, mockEnv),
                CLOCK
        );

        List<ChannelModelSupport> models = adapter.fetchModels(
                ProviderChannelId.of(1L),
                ProviderHost.of("http://127.0.0.1:" + server.getAddress().getPort()),
                ProviderKeyRef.of("test-key"),
                ProviderModelsPath.DEFAULT,
                Set.of(ProtocolType.OPENAI_RESPONSES),
                RoutePriority.of(10)
        );

        assertThat(models).hasSize(2);
        assertThat(models).extracting(model -> model.requestedModel().value())
                .containsExactly("gpt-4.1", "claude-sonnet");
        assertThat(models).extracting(ChannelModelSupport::upstreamProtocol)
                .containsOnly(ProtocolType.OPENAI_RESPONSES);
    }
}
