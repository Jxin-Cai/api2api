package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GatewayProtocolErrorBodyBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayProtocolErrorBodyBuilder builder = new GatewayProtocolErrorBodyBuilder(objectMapper);

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
