package com.api2api.ohs.http.gateway;

import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayInvocationOutcome;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.InboundRequestContext;
import com.api2api.application.gateway.ProtocolOperation;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.protocolcontract.acl.ExecutableProtocolContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return invokeProtocol(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolOperation.INVOKE,
                rawBody, authorization, apiKey, xRequestId, headers, httpRequest, httpResponse);
    }

    @PostMapping("/v1/messages/count_tokens")
    public Object claudeCountTokens(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return invokeProtocol(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolOperation.COUNT_TOKENS,
                rawBody, authorization, apiKey, xRequestId, headers, httpRequest, httpResponse);
    }

    /**
     * Codex-style Responses clients append {@code /responses} to a base_url that has no
     * {@code /v1} prefix, so the alias must resolve to the same endpoint.
     */
    @PostMapping({"/v1/responses", "/responses"})
    public Object openaiResponses(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return invokeProtocol(
                ProtocolType.OPENAI_RESPONSES,
                ProtocolOperation.INVOKE,
                rawBody, authorization, apiKey, xRequestId, headers, httpRequest, httpResponse);
    }

    @PostMapping("/v1/chat/completions")
    public Object openaiChatCompletions(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return invokeProtocol(
                ProtocolType.OPENAI_CHAT_COMPLETIONS,
                ProtocolOperation.INVOKE,
                rawBody, authorization, apiKey, xRequestId, headers, httpRequest, httpResponse);
    }

    /**
     * Accepts the inbound request in full — body, headers, query string and operation — and hands it
     * to the application layer unmodified. Deciding what a given provider may see belongs to the
     * upstream call policy, and reshaping payloads belongs to the protocol converters.
     */
    private Object invokeProtocol(
            ProtocolType protocol,
            ProtocolOperation operation,
            String rawBody,
            String authorization,
            String apiKey,
            String xRequestId,
            HttpHeaders headers,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        log.info("Received {} {} request, X-Request-Id: {}", protocol, operation, xRequestId);

        GatewayProtocolRequest protocolRequest = ContractBackedGatewayRequest.fromContract(
                protocolContract.parseGatewayRequest(protocol, rawBody)
        );
        InvokeGatewayCommand command = gatewayRequestMapper.toCommand(
                protocolRequest,
                authorization,
                apiKey,
                xRequestId,
                protocol,
                InboundRequestContext.of(headers, httpRequest.getQueryString(), operation)
        );
        logAcceptedRequest(command, xRequestId);

        if (command.isStreaming()) {
            return stream(command, httpResponse);
        }

        GatewayInvocationOutcome outcome = gatewayInvocationApplicationService.invokeOutcome(command);
        GatewayRawResponse rawResponse = responseMapper.toRawResponse(outcome);

        log.info("{} request completed, requestId: {}, status: {}",
                protocol, outcome.invocation().requestId().value(), outcome.invocation().result().status());

        return rawResponse.toResponseEntity();
    }

    private Object stream(InvokeGatewayCommand command, HttpServletResponse httpResponse) {
        GatewayStreamingInvocation streamingInvocation = gatewayInvocationApplicationService.openStreaming(command);
        if (!streamingInvocation.opened()) {
            return responseMapper
                    .toRawResponse(streamingInvocation.invocation(), streamingInvocation.upstreamMetadata())
                    .toResponseEntity();
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

}
