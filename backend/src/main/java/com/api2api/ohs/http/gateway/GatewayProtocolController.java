package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ObjectMapper objectMapper;

    @PostMapping("/v1/messages")
    public ResponseEntity<String> claudeMessages(
            @RequestBody String rawBody,
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {
        log.info("Received Claude Messages request, X-Request-Id: {}", xRequestId);

        JsonNode root = parseJsonBody(rawBody, ProtocolType.CLAUDE_MESSAGES);
        ClaudeMessagesGatewayRequest protocolRequest = ClaudeMessagesGatewayRequest.of(rawBody, root);
        rejectStreaming(protocolRequest, ProtocolType.CLAUDE_MESSAGES);
        InvokeGatewayCommand command = gatewayRequestMapper.toCommand(
                protocolRequest,
                authorization,
                xRequestId,
                ProtocolType.CLAUDE_MESSAGES
        );

        GatewayInvocation invocation = gatewayInvocationApplicationService.invoke(command);
        GatewayRawResponse rawResponse = responseMapper.toRawResponse(invocation);

        log.info("Claude Messages request completed, requestId: {}, status: {}",
                invocation.requestId().value(), invocation.result().status());

        return rawResponse.toResponseEntity();
    }

    @PostMapping("/v1/responses")
    public ResponseEntity<String> openaiResponses(
            @RequestBody String rawBody,
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {
        log.info("Received OpenAI Responses request, X-Request-Id: {}", xRequestId);

        JsonNode root = parseJsonBody(rawBody, ProtocolType.OPENAI_RESPONSES);
        OpenAIResponsesGatewayRequest protocolRequest = OpenAIResponsesGatewayRequest.of(rawBody, root);
        rejectStreaming(protocolRequest, ProtocolType.OPENAI_RESPONSES);
        InvokeGatewayCommand command = gatewayRequestMapper.toCommand(
                protocolRequest,
                authorization,
                xRequestId,
                ProtocolType.OPENAI_RESPONSES
        );

        GatewayInvocation invocation = gatewayInvocationApplicationService.invoke(command);
        GatewayRawResponse rawResponse = responseMapper.toRawResponse(invocation);

        log.info("OpenAI Responses request completed, requestId: {}, status: {}",
                invocation.requestId().value(), invocation.result().status());

        return rawResponse.toResponseEntity();
    }

    @PostMapping("/v1/chat/completions")
    public ResponseEntity<String> openaiChatCompletions(
            @RequestBody String rawBody,
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {
        log.info("Received OpenAI Chat Completions request, X-Request-Id: {}", xRequestId);

        JsonNode root = parseJsonBody(rawBody, ProtocolType.OPENAI_CHAT_COMPLETIONS);
        OpenAIChatCompletionsGatewayRequest protocolRequest = OpenAIChatCompletionsGatewayRequest.of(rawBody, root);
        rejectStreaming(protocolRequest, ProtocolType.OPENAI_CHAT_COMPLETIONS);
        InvokeGatewayCommand command = gatewayRequestMapper.toCommand(
                protocolRequest,
                authorization,
                xRequestId,
                ProtocolType.OPENAI_CHAT_COMPLETIONS
        );

        GatewayInvocation invocation = gatewayInvocationApplicationService.invoke(command);
        GatewayRawResponse rawResponse = responseMapper.toRawResponse(invocation);

        log.info("OpenAI Chat Completions request completed, requestId: {}, status: {}",
                invocation.requestId().value(), invocation.result().status());

        return rawResponse.toResponseEntity();
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

    private void rejectStreaming(GatewayProtocolRequest protocolRequest, ProtocolType protocol) {
        if (protocolRequest.streaming()) {
            throw GatewayProtocolException.badRequest(
                    protocol,
                    "Streaming is not supported by this gateway yet. Retry with stream=false."
            );
        }
    }
}
