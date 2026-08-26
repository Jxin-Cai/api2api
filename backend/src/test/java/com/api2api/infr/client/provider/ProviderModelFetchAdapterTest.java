package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.BusinessException;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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
    void test_sendsStoredKey_when_fetchingModels() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"data\":[{\"id\":\"claude-opus-4.6\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        adapter().fetchModels(
                ProviderChannelId.of(1L),
                ProviderHost.of("http://127.0.0.1:" + server.getAddress().getPort()),
                ProviderKeyRef.of("plaintext-provider-key"),
                ProviderModelsPath.DEFAULT,
                Set.of(ProtocolType.OPENAI_RESPONSES),
                RoutePriority.of(10)
        );

        assertThat(authorizationHeader.get()).isEqualTo("Bearer plaintext-provider-key");
    }

    @Test
    void test_fetchesModels_when_openAiListUsesDataAndId() throws IOException {
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

        List<ChannelModelSupport> models = adapter().fetchModels(
                ProviderChannelId.of(1L),
                ProviderHost.of("http://127.0.0.1:" + server.getAddress().getPort()),
                ProviderKeyRef.of("test-secret"),
                ProviderModelsPath.DEFAULT,
                Set.of(ProtocolType.OPENAI_RESPONSES),
                RoutePriority.of(10)
        );

        assertThat(models).hasSize(2);
        assertThat(models).extracting(model -> model.requestedModel().value())
                .containsExactly("claude-sonnet", "gpt-4.1");
        assertThat(models).extracting(ChannelModelSupport::upstreamProtocol)
                .containsOnly(ProtocolType.OPENAI_RESPONSES);
    }

    @Test
    void test_fetchesNewModels_when_hostAlreadyContainsV1AndListUsesModelsAliases() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            byte[] body = "{\"models\":[{\"slug\":\"gpt-5.5\"},{\"name\":\"models/gpt-5.5-mini\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ProviderModelFetchAdapter adapter = adapter();

        List<ChannelModelSupport> models = adapter.fetchModels(
                ProviderChannelId.of(1L),
                ProviderHost.of("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
                ProviderKeyRef.of("test-secret"),
                ProviderModelsPath.DEFAULT,
                Set.of(ProtocolType.OPENAI_RESPONSES),
                RoutePriority.of(10)
        );

        assertThat(models).extracting(model -> model.requestedModel().value())
                .containsExactly("gpt-5.5", "gpt-5.5-mini");
    }

    @Test
    void test_requestsDefaultModelsPath_when_customModelsPathProvided() throws IOException {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"data\":[{\"id\":\"gpt-4.1\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/foundation-models", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();

        adapter().fetchModels(
                ProviderChannelId.of(1L),
                ProviderHost.of("http://127.0.0.1:" + server.getAddress().getPort()),
                ProviderKeyRef.of("test-secret"),
                ProviderModelsPath.of("/foundation-models"),
                Set.of(ProtocolType.OPENAI_RESPONSES),
                RoutePriority.of(10)
        );

        assertThat(requestedPath.get()).isEqualTo("/v1/models");
        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-secret");
    }

    @Test
    void test_includesFailureReason_when_upstreamReturnsUnauthorized() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            byte[] body = "{\"error\":{\"message\":\"Incorrect API key provided\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> adapter().fetchModels(
                ProviderChannelId.of(1L),
                ProviderHost.of("http://127.0.0.1:" + server.getAddress().getPort()),
                ProviderKeyRef.of("test-secret"),
                ProviderModelsPath.of("/foundation-models"),
                Set.of(ProtocolType.OPENAI_RESPONSES),
                RoutePriority.of(10)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上游认证失败（HTTP 401）")
                .hasMessageContaining("/v1/models")
                .hasMessageContaining("Incorrect API key provided")
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo("PROVIDER_MODELS_AUTH_FAILED");
    }

    private ProviderModelFetchAdapter adapter() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setAllowInsecureHosts(true);
        return new ProviderModelFetchAdapter(
                properties,
                new UpstreamHttpHeaderPolicy(properties),
                new ObjectMapper(),
                new UpstreamUrlResolver(properties),
                CLOCK
        );
    }
}
