package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpstreamHttpHeaderPolicyTest {

    @Test
    void test_replacesClientCredentials_when_buildingUpstreamHeaders() {
        // Arrange
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(new ProviderHttpClientProperties());

        // Act
        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.OPENAI_RESPONSES,
                Map.of(
                        "Authorization", List.of("Bearer client-key"),
                        "x-api-key", List.of("client-key"),
                        "Cookie", List.of("sid=secret")
                ),
                "provider-secret",
                false
        );

        // Assert
        assertThat(headers)
                .containsEntry("Authorization", "Bearer provider-secret")
                .doesNotContainKeys("x-api-key", "Cookie");
    }

    @Test
    void test_forwardsClientFingerprintHeaders_when_notDenied() {
        // Arrange
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(new ProviderHttpClientProperties());

        // Act
        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.CLAUDE_MESSAGES,
                Map.of(
                        "User-Agent", List.of("claude-cli/1.0.0 (external, cli)"),
                        "X-Stainless-Lang", List.of("js"),
                        "X-App", List.of("cli")
                ),
                "provider-secret",
                false
        );

        // Assert
        assertThat(headers)
                .containsEntry("User-Agent", "claude-cli/1.0.0 (external, cli)")
                .containsEntry("X-Stainless-Lang", "js")
                .containsEntry("X-App", "cli");
    }

    @Test
    void test_dropsConnectionFramingHeaders_when_clientSendsThem() {
        // Arrange
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(new ProviderHttpClientProperties());

        // Act
        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.OPENAI_RESPONSES,
                Map.of(
                        "Host", List.of("gateway.internal"),
                        "Content-Length", List.of("42"),
                        "Accept-Encoding", List.of("gzip"),
                        "Connection", List.of("keep-alive")
                ),
                "provider-secret",
                false
        );

        // Assert
        assertThat(headers).doesNotContainKeys("Host", "Content-Length", "Accept-Encoding", "Connection");
    }

    @Test
    void test_mergesRepeatedValues_when_clientSplitsAnthropicBetaAcrossHeaderLines() {
        // Arrange
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(new ProviderHttpClientProperties());

        // Act
        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.CLAUDE_MESSAGES,
                Map.of("anthropic-beta", List.of("interleaved-thinking-2025-05-14", "context-1m-2025-08-07")),
                "provider-secret",
                false
        );

        // Assert
        assertThat(headers).containsEntry(
                "anthropic-beta", "interleaved-thinking-2025-05-14, context-1m-2025-08-07");
    }

    @Test
    void test_keepsClientAnthropicVersion_when_clientSuppliesOne() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setAnthropicVersion("2023-06-01");
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(properties);

        // Act
        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.CLAUDE_MESSAGES,
                Map.of("anthropic-version", List.of("2024-10-22")),
                "provider-secret",
                false
        );

        // Assert
        assertThat(headers).containsEntry("anthropic-version", "2024-10-22");
    }

    @Test
    void test_dropsForwardingHeaders_when_clientArrivesThroughProxy() {
        // Arrange
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(new ProviderHttpClientProperties());

        // Act
        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.CLAUDE_MESSAGES,
                Map.of(
                        "X-Forwarded-For", List.of("203.0.113.10"),
                        "X-Real-Ip", List.of("203.0.113.10"),
                        "Forwarded", List.of("for=203.0.113.10"),
                        "User-Agent", List.of("claude-cli/1.0.0 (external, cli)")
                ),
                "provider-secret",
                false
        );

        // Assert
        assertThat(headers)
                .containsEntry("User-Agent", "claude-cli/1.0.0 (external, cli)")
                .doesNotContainKeys("X-Forwarded-For", "X-Real-Ip", "Forwarded");
    }

    @Test
    void addsClaudeVersionAndStreamingAcceptHeader() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        properties.setAnthropicVersion("2023-06-01");
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(properties);

        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.CLAUDE_MESSAGES,
                Map.of(),
                "provider-secret",
                true
        );

        assertThat(headers).containsEntry("anthropic-version", "2023-06-01");
        assertThat(headers).containsEntry("Accept", "text/event-stream");
    }

    @Test
    void test_omitsAnthropicBetaHeader_when_targetUsesBedrockInvokeModelBody() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(properties);

        // Act
        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                Map.of("Anthropic-Beta", List.of("context-management-2025-06-27")),
                "provider-secret",
                false
        );

        // Assert
        assertThat(headers).doesNotContainKey("Anthropic-Beta");
    }
}
