package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.application.gateway.UpstreamResponseMetadata;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.gateway.model.GatewayInvocationId;
import com.api2api.domain.gateway.model.GatewayInvocationResult;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.gateway.model.InvocationError;
import com.api2api.domain.gateway.model.InvocationErrorType;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.routing.model.RouteFailure;
import com.api2api.domain.routing.model.RouteFailureType;
import com.api2api.domain.user.model.UserAccountId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GatewayInvocationResponseMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-12T00:00:00Z");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayProtocolErrorBodyBuilder errorBodyBuilder = new GatewayProtocolErrorBodyBuilder(objectMapper);
    private final GatewayInvocationResponseMapper mapper = new GatewayInvocationResponseMapper(objectMapper, errorBodyBuilder);

    @Test
    void test_returnsTooManyRequests_when_latestUpstreamFailureIsRateLimited() {
        // Arrange
        GatewayInvocation invocation = failedClaudeInvocation(RouteFailureType.RATE_LIMITED);

        // Act
        GatewayRawResponse response = mapper.toRawResponse(invocation);

        // Assert
        assertThat(response.statusCode()).isEqualTo(429);
    }

    @Test
    void test_returnsClaudeRateLimitError_when_latestUpstreamFailureIsRateLimited() throws Exception {
        // Arrange
        GatewayInvocation invocation = failedClaudeInvocation(RouteFailureType.RATE_LIMITED);

        // Act
        JsonNode body = objectMapper.readTree(mapper.toRawResponse(invocation).body());

        // Assert
        assertThat(body.at("/error/type").asText()).isEqualTo("rate_limit_error");
    }

    @Test
    void test_forwardsRetryAfterHeader_when_upstreamRateLimitedBeforeStreamOpened() {
        // Arrange
        GatewayInvocation invocation = failedClaudeInvocation(RouteFailureType.RATE_LIMITED);
        UpstreamResponseMetadata upstreamMetadata = UpstreamResponseMetadata.of(
                Map.of("retry-after", List.of("30"), "x-ratelimit-remaining", List.of("0")));

        // Act
        GatewayRawResponse response = mapper.toRawResponse(invocation, upstreamMetadata);

        // Assert
        assertThat(response.headers()).containsEntry("retry-after", List.of("30"));
    }

    private GatewayInvocation failedClaudeInvocation(RouteFailureType failureType) {
        RouteFailure failure = RouteFailure.of(
                ProviderChannelId.of(1L),
                failureType,
                "Upstream returned HTTP 429",
                true,
                NOW
        );
        InvocationError error = InvocationError.of(
                InvocationErrorType.UPSTREAM_FAILED,
                failure.failureType() + ": " + failure.reason(),
                List.of(failure)
        );
        GatewayInvocation invocation = GatewayInvocation.start(
                GatewayInvocationId.of(1L),
                GatewayRequestId.of("request-1"),
                UserAccountId.of(1L),
                ApiCredentialId.of(1L),
                ProtocolType.CLAUDE_MESSAGES,
                ModelName.of("claude-opus-4-6"),
                ConversionRequirement.of(true, true, true),
                NOW
        );
        invocation.fail(GatewayInvocationResult.failed(error, true), NOW);
        return invocation;
    }
}
