package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.api2api.domain.channel.model.ProtocolType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GatewayProtocolErrorBodyBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayProtocolErrorBodyBuilder builder = new GatewayProtocolErrorBodyBuilder(objectMapper);

    @ParameterizedTest
    @EnumSource(value = ProtocolType.class, names = {"CLAUDE_MESSAGES", "OPENAI_RESPONSES"})
    void test_returnsProtocolFallback_when_jsonSerializationFails(ProtocolType protocol) throws Exception {
        // Arrange
        ObjectMapper failingMapper = spy(new ObjectMapper());
        doThrow(new JsonProcessingException("synthetic serialization failure") { })
                .when(failingMapper).writeValueAsString(any());
        var failingBuilder = new GatewayProtocolErrorBodyBuilder(failingMapper);
        String expected = protocol == ProtocolType.CLAUDE_MESSAGES
                ? "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"Internal server error\"}}"
                : "{\"error\":{\"message\":\"Internal server error\",\"type\":\"api_error\"}}";

        // Act
        String body = buildErrorBody(failingBuilder, protocol);

        // Assert
        assertThat(objectMapper.readTree(body)).isEqualTo(objectMapper.readTree(expected));
    }

    @ParameterizedTest
    @EnumSource(value = ProtocolType.class, names = {"CLAUDE_MESSAGES", "OPENAI_RESPONSES"})
    void test_propagatesUnexpectedFailure_when_jsonMapperHasProgrammingError(ProtocolType protocol) {
        // Arrange
        ObjectMapper failingMapper = spy(new ObjectMapper());
        var failure = new IllegalStateException("synthetic mapper failure");
        doThrow(failure).when(failingMapper).createObjectNode();
        var failingBuilder = new GatewayProtocolErrorBodyBuilder(failingMapper);

        // Act / Assert
        assertThatThrownBy(() -> buildErrorBody(failingBuilder, protocol)).isSameAs(failure);
    }

    private String buildErrorBody(GatewayProtocolErrorBodyBuilder target, ProtocolType protocol) {
        return protocol == ProtocolType.CLAUDE_MESSAGES
                ? target.buildClaudeErrorBody("api_error", "failure")
                : target.buildOpenAIErrorBody("api_error", "failure");
    }

    @Test
    void test_returnsClaudeErrorShape_when_buildClaudeErrorBodyCalled() throws Exception {
        // Arrange
        String errorType = "authentication_error";
        String message = "Invalid API key";

        // Act
        String body = builder.buildClaudeErrorBody(errorType, message);

        // Assert
        JsonNode root = objectMapper.readTree(body);
        assertThat(root.path("type").asText()).isEqualTo("error");
        assertThat(root.at("/error/type").asText()).isEqualTo("authentication_error");
        assertThat(root.at("/error/message").asText()).isEqualTo("Invalid API key");
    }

    @Test
    void test_defaultsErrorTypeToInvalidRequestError_when_buildClaudeErrorBodyCalledWithBlankType() throws Exception {
        // Arrange / Act
        String body = builder.buildClaudeErrorBody("", "some message");

        // Assert
        JsonNode root = objectMapper.readTree(body);
        assertThat(root.at("/error/type").asText()).isEqualTo("invalid_request_error");
    }

    @Test
    void test_returnsOpenAIErrorShape_when_buildOpenAIErrorBodyCalled() throws Exception {
        // Arrange
        String errorType = "rate_limit_error";
        String message = "Rate limit exceeded";

        // Act
        String body = builder.buildOpenAIErrorBody(errorType, message);

        // Assert
        JsonNode root = objectMapper.readTree(body);
        assertThat(root.path("type").isMissingNode()).isTrue();
        assertThat(root.at("/error/message").asText()).isEqualTo("Rate limit exceeded");
        assertThat(root.at("/error/type").asText()).isEqualTo("rate_limit_error");
        assertThat(root.at("/error/param").isNull()).isTrue();
        assertThat(root.at("/error/code").isNull()).isTrue();
    }

    @Test
    void test_defaultsErrorTypeToInvalidRequestError_when_buildOpenAIErrorBodyCalledWithBlankType() throws Exception {
        // Arrange / Act
        String body = builder.buildOpenAIErrorBody(null, "some message");

        // Assert
        JsonNode root = objectMapper.readTree(body);
        assertThat(root.at("/error/type").asText()).isEqualTo("invalid_request_error");
    }
}
