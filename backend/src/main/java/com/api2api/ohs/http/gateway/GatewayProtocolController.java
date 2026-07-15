package com.api2api.ohs.http.gateway;

import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayInvocationOutcome;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.protocolcontract.acl.ExecutableProtocolContract;
import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import com.api2api.domain.protocolcontract.model.ProtocolContractViolationException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    private static final String MODEL_OBJECT_TYPE = "model";
    private static final String MODEL_LIST_OBJECT_TYPE = "list";
    private static final String MODEL_OWNER = "api2api";

    @NonNull
    private final ApiCredentialApplicationService apiCredentialApplicationService;

    @NonNull
    private final GatewayApiKeyHashHelper apiKeyHashHelper;

    @NonNull
    private final GatewayInvocationApplicationService gatewayInvocationApplicationService;

    @NonNull
    private final GatewayRequestMapper gatewayRequestMapper;

    @NonNull
    private final GatewayInvocationResponseMapper responseMapper;

    @NonNull
    private final GatewayStreamingResponseMapper streamingResponseMapper;

    @NonNull
    private final ExecutableProtocolContract protocolContract;

    @GetMapping({"/v1/model", "/v1/models"})
    public GatewayModelListResponse listModels(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey
    ) {
        ApiKeyHash keyHash = apiKeyHashHelper.hashGatewayApiKey(authorization, apiKey);
        ApiCredential credential = apiCredentialApplicationService.authenticateForModelListing(keyHash);
        long createdAt = credential.getCreatedAt().getEpochSecond();
        List<GatewayModelResponse> models = credential.getModelWhitelist().models().stream()
                .map(ModelName::value)
                .sorted()
                .map(model -> new GatewayModelResponse(
                        model,
                        MODEL_OBJECT_TYPE,
                        createdAt,
                        MODEL_OWNER
                ))
                .toList();
        return new GatewayModelListResponse(MODEL_LIST_OBJECT_TYPE, models);
    }

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

        ClaudeMessagesGatewayRequest protocolRequest = ClaudeMessagesGatewayRequest.fromContract(
                protocolContract.parseRequest(ProtocolType.CLAUDE_MESSAGES, rawBody)
        );
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

        OpenAIResponsesGatewayRequest protocolRequest = OpenAIResponsesGatewayRequest.fromContract(
                protocolContract.parseRequest(ProtocolType.OPENAI_RESPONSES, rawBody)
        );
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

        OpenAIChatCompletionsGatewayRequest protocolRequest = OpenAIChatCompletionsGatewayRequest.fromContract(
                protocolContract.parseRequest(ProtocolType.OPENAI_CHAT_COMPLETIONS, rawBody)
        );
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

    private void validateContractRequest(ProtocolType protocolType, String rawBody) {
        try {
            protocolContract.parseRequest(protocolType, rawBody);
        } catch (ProtocolContractViolationException exception) {
            throw GatewayProtocolException.badRequest(protocolType, exception.getMessage());
        }
    }
}
