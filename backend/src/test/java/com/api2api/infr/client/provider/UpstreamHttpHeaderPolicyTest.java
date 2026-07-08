package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpstreamHttpHeaderPolicyTest {

    @Test
    void appliesDenylistBeforePassthroughHeadersAndInjectsProviderAuthorization() {
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();
        UpstreamHttpHeaderPolicy policy = new UpstreamHttpHeaderPolicy(properties);

        Map<String, String> headers = policy.buildHeaders(
                ProtocolType.OPENAI_RESPONSES,
                Map.of(
                        "Authorization", List.of("Bearer client-key"),
                        "Cookie", List.of("sid=secret"),
                        "X-Request-Id", List.of("request-1"),
                        "Traceparent", List.of("00-abc-def-01"),
                        "X-Not-Allowed", List.of("ignored")
                ),
                "provider-secret",
                false
        );

        assertThat(headers).containsEntry("Authorization", "Bearer provider-secret");
        assertThat(headers).containsEntry("X-Request-Id", "request-1");
        assertThat(headers).containsEntry("Traceparent", "00-abc-def-01");
        assertThat(headers).doesNotContainKey("Cookie");
        assertThat(headers).doesNotContainKey("X-Not-Allowed");
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
}
