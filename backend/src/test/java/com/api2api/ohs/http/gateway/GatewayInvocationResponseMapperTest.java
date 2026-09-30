package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.api2api.application.gateway.GatewayInvocationOutcome;
import com.api2api.application.gateway.ProviderGatewayResponse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GatewayInvocationResponseMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-12T00:00:00Z");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayProtocolErrorBodyBuilder errorBodyBuilder = new GatewayProtocolErrorBodyBuilder(objectMapper);
    private final GatewayInvocationResponseMapper mapper = new GatewayInvocationResponseMapper(objectMapper, errorBodyBuilder);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "upstream unavailable | upstream unavailable",
            "{\"error\":{\"message\":\"rate limited\"}} | rate limited",
            "{\"detail\":\"rate limited\"} | rate limited",
            "null | null"
    })
    void test_preservesUpstreamErrorMessage_when_translatingFailure(String upstreamBody, String expected) throws Exception {
        // Arrange
        var outcome = failedProviderOutcome(upstreamBody);

        // Act
        JsonNode body = objectMapper.readTree(mapper.toRawResponse(outcome).body());

        // Assert
        assertThat(body.at("/error/message").asText()).isEqualTo(expected);
    }

    @Test
    void test_propagatesUnexpectedFailure_when_upstreamErrorParserFails() throws Exception {
        // Arrange
        var failure = new IllegalStateException("synthetic parser failure");
        var failingMapper = spy(new ObjectMapper());
        doThrow(failure).when(failingMapper).readTree(anyString());
        var responseMapper = new GatewayInvocationResponseMapper(failingMapper, errorBodyBuilder);
        var outcome = failedProviderOutcome("{}");

        // Act / Assert
        assertThatThrownBy(() -> responseMapper.toRawResponse(outcome)).isSameAs(failure);
    }

    @Test
    void test_propagatesUnexpectedFailure_when_errorBodyBuilderFails() {
        // Arrange
        var failure = new IllegalStateException("synthetic builder failure");
        var failingBuilder = spy(errorBodyBuilder);
        doThrow(failure).when(failingBuilder).buildClaudeErrorBody(anyString(), anyString());
        var responseMapper = new GatewayInvocationResponseMapper(objectMapper, failingBuilder);
        var invocation = failedClaudeInvocation(RouteFailureType.RATE_LIMITED);

        // Act / Assert
        assertThatThrownBy(() -> responseMapper.toRawResponse(invocation)).isSameAs(failure);
    }

    private GatewayInvocationOutcome failedProviderOutcome(String body) {
        return GatewayInvocationOutcome.of(failedClaudeInvocation(RouteFailureType.RATE_LIMITED),
                ProviderGatewayResponse.of(ProtocolType.OPENAI_RESPONSES, 429, Map.of(), body, false));
    }

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
                null,
                ConversionRequirement.of(true, true, true),
                NOW
        );
        invocation.fail(GatewayInvocationResult.failed(error, true), NOW);
        return invocation;
    }
}
