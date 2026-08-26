package com.api2api.infr.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.BusinessException;
import com.api2api.application.evaluation.ProbeRunSnapshot;
import com.api2api.application.evaluation.ProbeSubmission;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import com.api2api.domain.evaluation.model.ProbeUpstreamFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ChannelEvaluationProbeAdapterTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void test_submits_async_run_when_probe_service_returns_run_id() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/probe/run", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"runId\":\"run-42\",\"status\":\"running\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        ChannelEvaluationProbeAdapter adapter = adapter("http://127.0.0.1:" + server.getAddress().getPort());

        String runId = adapter.submit(ProbeSubmission.builder()
                .host(ProviderHost.of("https://api.example.com"))
                .keyRef(ProviderKeyRef.of("plaintext-key"))
                .modelId(ModelName.of("gpt-4o"))
                .upstreamFormat(ProbeUpstreamFormat.OPENAI)
                .build());

        assertThat(runId).isEqualTo("run-42");
        assertThat(body.get()).contains("\"sync\":false");
        assertThat(body.get()).contains("\"modelId\":\"gpt-4o\"");
        assertThat(body.get()).contains("\"apiKey\":\"plaintext-key\"");
        assertThat(body.get()).contains("\"upstreamFormat\":\"openai\"");
    }

    @Test
    void test_reads_completed_history_when_live_run_is_gone() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/probe/run/run-42", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/api/probe/history/run-42", exchange -> {
            byte[] response = """
                    {
                      "runId":"run-42",
                      "status":"completed",
                      "score":87,
                      "scoreMax":100,
                      "completedAt":"2026-08-21T00:01:00Z",
                      "items":[{"probeId":"zh_reasoning","passed":true}]
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        ChannelEvaluationProbeAdapter adapter = adapter("http://127.0.0.1:" + server.getAddress().getPort());

        ProbeRunSnapshot snapshot = adapter.fetch("run-42");

        assertThat(snapshot.status()).isEqualTo(EvaluationStatus.SUCCEEDED);
        assertThat(snapshot.findOutcome()).isPresent();
        assertThat(snapshot.findOutcome().orElseThrow().score().value()).isEqualByComparingTo("87.00");
        assertThat(snapshot.findOutcome().orElseThrow().passedProbeCount()).isEqualTo(1);
    }

    @Test
    void test_fails_submit_when_probe_service_omits_run_id() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/probe/run", exchange -> {
            byte[] response = "{\"status\":\"running\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        ChannelEvaluationProbeAdapter adapter = adapter("http://127.0.0.1:" + server.getAddress().getPort());

        assertThatThrownBy(() -> adapter.submit(ProbeSubmission.builder()
                .host(ProviderHost.of("https://api.example.com"))
                .keyRef(ProviderKeyRef.of("plaintext-key"))
                .modelId(ModelName.of("gpt-4o"))
                .upstreamFormat(ProbeUpstreamFormat.OPENAI)
                .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).code())
                .isEqualTo("EVALUATION_PROBE_RUN_ID_MISSING");
    }

    private ChannelEvaluationProbeAdapter adapter(String baseUrl) {
        EvaluationProbeProperties properties = new EvaluationProbeProperties();
        properties.setBaseUrl(baseUrl);
        ObjectMapper objectMapper = new ObjectMapper();
        return new ChannelEvaluationProbeAdapter(
                properties,
                new ProbeReportCompactor(objectMapper, properties),
                objectMapper
        );
    }
}
