package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.application.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class GatewayProtocolExceptionAdviceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayProtocolErrorBodyBuilder errorBodyBuilder = new GatewayProtocolErrorBodyBuilder(objectMapper);
    private final GatewayProtocolExceptionAdvice advice = new GatewayProtocolExceptionAdvice(errorBodyBuilder);

    @Test
    void test_returnsClaudeBadRequestError_when_messagesRequestIsInvalid() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/messages");

        ResponseEntity<String> response = advice.handleBadRequest(
                new IllegalArgumentException("Model is required in protocol request"),
                request
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.path("type").asText()).isEqualTo("error");
        assertThat(body.at("/error/type").asText()).isEqualTo("invalid_request_error");
        assertThat(body.at("/error/message").asText()).isEqualTo("Model is required in protocol request");
        assertThat(body.at("/error/param").isMissingNode()).isTrue();
    }

    @Test
    void test_returnsClaudeAuthenticationError_when_apiCredentialIsInvalid() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/messages");

        ResponseEntity<String> response = advice.handleBusiness(
                new BusinessException("API_CREDENTIAL_INVALID"),
                request
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body.path("type").asText()).isEqualTo("error");
        assertThat(body.at("/error/type").asText()).isEqualTo("authentication_error");
    }

    @Test
    void test_returnsClaudePermissionError_when_modelIsNotAllowed() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/messages");

        ResponseEntity<String> response = advice.handleBadRequest(
                new IllegalStateException("MODEL_NOT_ALLOWED: requested model is not allowed by API credential whitelist"),
                request
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body.path("type").asText()).isEqualTo("error");
        assertThat(body.at("/error/type").asText()).isEqualTo("permission_error");
    }

    @Test
    void test_returnsClaudeRateLimitError_when_quotaIsExhausted() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/messages");

        ResponseEntity<String> response = advice.handleBadRequest(
                new IllegalStateException("TOKEN_QUOTA_EXHAUSTED: API credential token quota is exhausted"),
                request
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(body.path("type").asText()).isEqualTo("error");
        assertThat(body.at("/error/type").asText()).isEqualTo("rate_limit_error");
    }

    @Test
    void test_returnsRateLimitError_when_modelDailyLimitIsExceeded() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/chat/completions");

        ResponseEntity<String> response = advice.handleBadRequest(
                new IllegalStateException("MODEL_DAILY_LIMIT_EXCEEDED: daily token limit of model gpt-4.1 has been reached"),
                request
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(body.at("/error/type").asText()).isEqualTo("rate_limit_error");
    }

    @Test
    void test_returnsOpenAIErrorShape_when_responsesRequestIsInvalid() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/responses");

        ResponseEntity<String> response = advice.handleBadRequest(
                new IllegalArgumentException("Invalid request"),
                request
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.path("type").isMissingNode()).isTrue();
        assertThat(body.at("/error/type").asText()).isEqualTo("invalid_request_error");
        assertThat(body.at("/error/message").asText()).isEqualTo("Invalid request");
        assertThat(body.at("/error/param").isNull()).isTrue();
        assertThat(body.at("/error/code").isNull()).isTrue();
    }

    @Test
    void test_returnsOpenAIErrorShape_when_imagesEditsRequestIsInvalid() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/images/edits");

        ResponseEntity<String> response = advice.handleBadRequest(
                new IllegalArgumentException("Request body must be multipart/form-data"),
                request
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.path("type").isMissingNode()).isTrue();
        assertThat(body.at("/error/type").asText()).isEqualTo("invalid_request_error");
    }

    @Test
    void test_returnsClaudeApiError_when_unexpectedExceptionOccurs() throws Exception {
        MockHttpServletRequest request = requestFor("/v1/messages");

        ResponseEntity<String> response = advice.handleUnexpected(new RuntimeException("boom"), request);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.path("type").asText()).isEqualTo("error");
        assertThat(body.at("/error/type").asText()).isEqualTo("api_error");
        assertThat(body.at("/error/message").asText()).isEqualTo("Internal server error");
    }

    @Test
    void test_returnsNoErrorBody_when_clientDisconnectsDuringStreaming() {
        // Arrange
        MockHttpServletRequest request = requestFor("/v1/messages");
        AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException(
                "ServletOutputStream failed to flush: java.io.IOException: Broken pipe",
                new IOException("Broken pipe")
        );

        // Act
        ResponseEntity<String> response = advice.handleUnexpected(exception, request);

        // Assert
        assertThat(response).isNull();
    }

    private MockHttpServletRequest requestFor(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
