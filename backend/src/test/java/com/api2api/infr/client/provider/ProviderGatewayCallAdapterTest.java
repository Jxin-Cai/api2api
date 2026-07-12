package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.application.gateway.UpstreamGatewayException;
import com.api2api.domain.channel.model.ChannelProtocolMapping;
import com.api2api.domain.channel.model.ModelMappingResult;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderChannelStatus;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ConversionImplementationStatus;
import com.api2api.domain.protocol.model.ConversionRoute;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingDirection;
import com.api2api.domain.protocol.model.MappingDocument;
import com.api2api.domain.protocol.model.MappingLossiness;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.routing.model.RouteCandidate;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProviderGatewayCallAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T00:00:00Z");
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void forwardReturnsRawStatusHeadersAndBodyForSuccessfulResponse() throws IOException {
        server = server(200, "application/json", "{\"ok\":true}");
        ProviderGatewayCallAdapter adapter = adapter();

        ProviderGatewayResponse response = adapter.forward(
                candidate(ProtocolType.OPENAI_RESPONSES),
                "{\"model\":\"gpt\"}",
                false,
                Map.of("X-Request-Id", List.of("request-1"), "Authorization", List.of("Bearer client-key"))
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"ok\":true}");
        assertThat(response.headers()).containsKey("content-type");
    }

    @Test
    void forwardReturnsRawErrorResponseWithoutThrowing() throws IOException {
        server = server(401, "application/json", "{\"error\":\"bad key\"}");
        ProviderGatewayCallAdapter adapter = adapter();

        ProviderGatewayResponse response = adapter.forward(
                candidate(ProtocolType.OPENAI_RESPONSES),
                "{}",
                false,
                Map.of()
        );

        assertThat(response.successful()).isFalse();
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("bad key");
    }

    @Test
    void openStreamReturnsStatusHeadersAndBody() throws IOException {
        server = server(200, "text/event-stream", "data: hello\n\n");
        ProviderGatewayCallAdapter adapter = adapter();

        ProviderStreamingResponse response = adapter.openStream(
                candidate(ProtocolType.OPENAI_RESPONSES),
                "{}",
                Map.of()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers()).containsKey("content-type");
        assertThat(new String(response.body().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("data: hello\n\n");
        response.close();
    }

    @Test
    void openStreamTreatsModelNotFoundAsRetryableForProtocolFallback() throws IOException {
        server = server(404, "application/json", "{\"error\":{\"type\":\"model_not_found\",\"message\":\"not supported by any configured account\"}}");
        ProviderGatewayCallAdapter adapter = adapter();

        UpstreamGatewayException exception = Assertions.catchThrowableOfType(
                () -> adapter.openStream(candidate(ProtocolType.OPENAI_RESPONSES), "{}", Map.of()),
                UpstreamGatewayException.class
        );

        assertThat(exception.statusCode()).isEqualTo(404);
        assertThat(exception.retryable()).isTrue();
    }

    @Test
    void test_doesNotRetryStreamingRequest_when_upstreamRateLimits() throws IOException {
        // Arrange
        AtomicInteger requests = new AtomicInteger();
        server = serverThatRateLimitsOnce(requests);
        ProviderGatewayCallAdapter adapter = adapterWithStreamingRetryBackoff();

        // Act
        UpstreamGatewayException exception = Assertions.catchThrowableOfType(
                () -> adapter.openStream(candidate(ProtocolType.OPENAI_RESPONSES), "{}", Map.of()),
                UpstreamGatewayException.class
        );

        // Assert
        assertThat(exception.failureType()).isEqualTo(com.api2api.domain.routing.model.RouteFailureType.RATE_LIMITED);
        assertThat(requests.get()).isEqualTo(1);
    }

    private HttpServer server(int status, String contentType, String body) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/v1/responses", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer provider-secret");
            assertThat(exchange.getRequestHeaders().getFirst("X-Request-Id")).isNotEqualTo("Bearer client-key");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        httpServer.start();
        return httpServer;
    }

    private HttpServer serverThatRateLimitsOnce(AtomicInteger requests) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/v1/responses", exchange -> {
            int requestNumber = requests.incrementAndGet();
            int status = requestNumber == 1 ? 429 : 200;
            String body = requestNumber == 1 ? "{\"error\":\"busy\"}" : "data: ready\n\n";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", requestNumber == 1
                    ? "application/json"
                    : "text/event-stream");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        httpServer.start();
        return httpServer;
    }

    private ProviderGatewayCallAdapter adapter() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setAllowInsecureHosts(true);
        return adapter(properties);
    }

    private ProviderGatewayCallAdapter adapterWithStreamingRetryBackoff() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setAllowInsecureHosts(true);
        properties.setStreamingRetryBackoff(java.time.Duration.ofMillis(1));
        return adapter(properties);
    }

    private ProviderGatewayCallAdapter adapter(ProviderHttpClientProperties properties) {
        ProviderSecretProperties secretProperties = new ProviderSecretProperties();
        secretProperties.setKeys(Map.of("provider-key", "provider-secret"));
        ProviderChannelRepository repo = new FixedProviderChannelRepository(channel());
        ProviderSecretResolver secretResolver = new ProviderSecretResolver(secretProperties, new MockEnvironment());
        BearerTokenProviderCallStrategy bearerStrategy = new BearerTokenProviderCallStrategy(
                repo,
                secretResolver,
                properties,
                new UpstreamHttpHeaderPolicy(properties),
                new UpstreamUrlResolver(properties)
        );
        return new ProviderGatewayCallAdapter(List.of(bearerStrategy));
    }

    private ProviderChannel channel() {
        return ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("test"),
                ProviderHost.of("http://127.0.0.1:" + server.getAddress().getPort()),
                ProviderKeyRef.of("provider-key"),
                ProviderModelsPath.DEFAULT,
                1,
                Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                List.of(),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );
    }

    private RouteCandidate candidate(ProtocolType protocol) {
        ModelName model = ModelName.of("gpt");
        return RouteCandidate.of(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("test"),
                model,
                model,
                protocol,
                protocol,
                RoutePriority.of(1),
                1,
                false,
                ConversionRoute.of(definition(protocol), protocol, protocol),
                ModelMappingResult.of(model, model)
        );
    }

    private ProtocolConversionDefinition definition(ProtocolType protocol) {
        return ProtocolConversionDefinition.create(
                ProtocolConversionDefinitionId.of(1L),
                protocol,
                protocol,
                ConversionCapability.of(true, true, true, true, true, Set.of(ContentMappingType.TEXT, ContentMappingType.TOOL_CALL)),
                mapping(MappingDirection.REQUEST),
                mapping(MappingDirection.RESPONSE),
                ConversionImplementationStatus.IMPLEMENTED,
                NOW
        );
    }

    private MappingDocument mapping(MappingDirection direction) {
        return MappingDocument.of(
                direction,
                direction.name() + " passthrough",
                "passthrough",
                List.of(FieldMapping.of("payload", "payload", "passthrough", MappingLossiness.NONE))
        );
    }

    private static final class FixedProviderChannelRepository implements ProviderChannelRepository {
        private final ProviderChannel channel;

        private FixedProviderChannelRepository(ProviderChannel channel) {
            this.channel = channel;
        }

        @Override
        public void save(ProviderChannel providerChannel) {
        }

        @Override
        public Optional<ProviderChannel> findById(ProviderChannelId id) {
            return Optional.of(channel);
        }

        @Override
        public List<ProviderChannel> findAll() {
            return List.of(channel);
        }

        @Override
        public List<ProviderChannel> findEnabledForRouting() {
            return List.of(channel);
        }

        @Override
        public void markRateLimited(ProviderChannelId id, java.time.Instant isolatedAt) {
        }

        @Override
        public int restoreRateLimitedBefore(java.time.Instant cutoff, java.time.Instant restoredAt) {
            return 0;
        }

        @Override
        public void softDeleteById(ProviderChannelId id, java.time.Instant deletedAt) {
        }
    }
}
