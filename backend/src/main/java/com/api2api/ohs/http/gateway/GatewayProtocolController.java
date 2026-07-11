package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayInvocationOutcome;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway controller exposing three protocol-compatible endpoints for external SDKs.
 * Returns raw protocol responses without management API wrapping.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class GatewayProtocolController {

    @NonNull
    private final GatewayInvocationApplicationService gatewayInvocationApplicationService;

    @NonNull
    private final GatewayRequestMapper gatewayRequestMapper;

    @NonNull
    private final GatewayInvocationResponseMapper responseMapper;

    @NonNull
    private final GatewayStreamingResponseMapper streamingResponseMapper;

    @NonNull
    private final ObjectMapper objectMapper;

    @PostMapping("/v1/messages")
    public Object claudeMessages(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletResponse httpResponse
    ) {
        log.info("Received Claude Messages request, X-Request-Id: {}", xRequestId);

        JsonNode root = parseJsonBody(rawBody, ProtocolType.CLAUDE_MESSAGES);
        ClaudeMessagesGatewayRequest protocolRequest = ClaudeMessagesGatewayRequest.of(rawBody, root);
        InvokeGatewayCommand command = gatewayRequestMapper.toCommand(
                protocolRequest,
                authorization,
                apiKey,
                xRequestId,
                ProtocolType.CLAUDE_MESSAGES,
                headers
        );
        logAcceptedRequest(command, xRequestId);

        if (command.isStreaming()) {
            return stream(command, httpResponse);
        }

        GatewayInvocationOutcome outcome = gatewayInvocationApplicationService.invokeOutcome(command);
        GatewayRawResponse rawResponse = responseMapper.toRawResponse(outcome);

        log.info("Claude Messages request completed, requestId: {}, status: {}",
                outcome.invocation().requestId().value(), outcome.invocation().result().status());

        return rawResponse.toResponseEntity();
    }

    @PostMapping("/v1/responses")
    public Object openaiResponses(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletResponse httpResponse
    ) {
        log.info("Received OpenAI Responses request, X-Request-Id: {}", xRequestId);

        JsonNode root = parseJsonBody(rawBody, ProtocolType.OPENAI_RESPONSES);
        OpenAIResponsesGatewayRequest protocolRequest = OpenAIResponsesGatewayRequest.of(rawBody, root);
        InvokeGatewayCommand command = gatewayRequestMapper.toCommand(
                protocolRequest,
                authorization,
                apiKey,
                xRequestId,
                ProtocolType.OPENAI_RESPONSES,
                headers
        );
        logAcceptedRequest(command, xRequestId);

        if (command.isStreaming()) {
            return stream(command, httpResponse);
        }

        GatewayInvocationOutcome outcome = gatewayInvocationApplicationService.invokeOutcome(command);
        GatewayRawResponse rawResponse = responseMapper.toRawResponse(outcome);

        log.info("OpenAI Responses request completed, requestId: {}, status: {}",
                outcome.invocation().requestId().value(), outcome.invocation().result().status());

        return rawResponse.toResponseEntity();
    }

    @PostMapping("/v1/chat/completions")
    public Object openaiChatCompletions(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletResponse httpResponse
    ) {
        log.info("Received OpenAI Chat Completions request, X-Request-Id: {}", xRequestId);

        JsonNode root = parseJsonBody(rawBody, ProtocolType.OPENAI_CHAT_COMPLETIONS);
        OpenAIChatCompletionsGatewayRequest protocolRequest = OpenAIChatCompletionsGatewayRequest.of(rawBody, root);
        InvokeGatewayCommand command = gatewayRequestMapper.toCommand(
                protocolRequest,
                authorization,
                apiKey,
                xRequestId,
                ProtocolType.OPENAI_CHAT_COMPLETIONS,
                headers
        );
        logAcceptedRequest(command, xRequestId);

        if (command.isStreaming()) {
            return stream(command, httpResponse);
        }

        GatewayInvocationOutcome outcome = gatewayInvocationApplicationService.invokeOutcome(command);
        GatewayRawResponse rawResponse = responseMapper.toRawResponse(outcome);

        log.info("OpenAI Chat Completions request completed, requestId: {}, status: {}",
                outcome.invocation().requestId().value(), outcome.invocation().result().status());

        return rawResponse.toResponseEntity();
    }

    private Object stream(InvokeGatewayCommand command, HttpServletResponse httpResponse) {
        GatewayStreamingInvocation streamingInvocation = gatewayInvocationApplicationService.openStreaming(command);
        if (!streamingInvocation.opened()) {
            return responseMapper.toRawResponse(streamingInvocation.invocation()).toResponseEntity();
        }
        return streamingResponseMapper.toResponseBody(streamingInvocation, httpResponse);
    }

    private void logAcceptedRequest(InvokeGatewayCommand command, String incomingRequestId) {
        log.info(
                "Gateway request accepted, requestId: {}, incomingXRequestId: {}, protocol: {}, model: {}, streaming: {}",
                command.getGatewayRequestId().value(),
                incomingRequestId,
                command.getRequestProtocol(),
                command.getRequestedModel().value(),
                command.isStreaming()
        );
    }

    private JsonNode parseJsonBody(String rawBody, ProtocolType protocol) {
        if (rawBody == null || rawBody.isBlank()) {
            throw GatewayProtocolException.badRequest(protocol, "Request body must not be empty");
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw GatewayProtocolException.badRequest(protocol, "Invalid JSON request body");
        }
    }
}
