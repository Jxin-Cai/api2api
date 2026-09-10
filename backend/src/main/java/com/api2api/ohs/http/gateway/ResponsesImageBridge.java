package com.api2api.ohs.http.gateway;

import com.api2api.application.BusinessException;
import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.credential.command.AuthenticateApiCredentialCommand;
import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.InboundRequestContext;
import com.api2api.application.gateway.ProtocolOperation;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.application.gateway.StreamingPassthroughPort;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.protocolcontract.acl.ExecutableProtocolContract;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Endpoint orchestration: select a tool with the main model, invoke Images through the existing
 * gateway, then replace only the internal function calls with public image_generation_call items.
 * Both model invocations retain independent credentials, quota reservations and usage records.
 */
@Slf4j
@Component
public class ResponsesImageBridge {
    private final ObjectMapper mapper;
    private final ResponsesImagePayloadMapper payloadMapper;
    private final GatewayInvocationApplicationService gateway;
    private final GatewayRequestMapper requestMapper;
    private final GatewayIdentifierHelper identifiers;
    private final GatewayInvocationResponseMapper responseMapper;
    private final ExecutableProtocolContract contract;
    private final ApiCredentialApplicationService credentials;
    private final StreamingPassthroughPort passthrough;
    private final String defaultImageModel;

    public ResponsesImageBridge(ObjectMapper mapper, ResponsesImagePayloadMapper payloadMapper,
            GatewayInvocationApplicationService gateway, GatewayRequestMapper requestMapper,
            GatewayIdentifierHelper identifiers, GatewayInvocationResponseMapper responseMapper,
            ExecutableProtocolContract contract, ApiCredentialApplicationService credentials,
            StreamingPassthroughPort passthrough,
            @Value("${api2api.gateway.responses-image-model:gpt-image-2.5}") String defaultImageModel) {
        this.mapper = mapper;
        this.payloadMapper = payloadMapper;
        this.gateway = gateway;
        this.requestMapper = requestMapper;
        this.identifiers = identifiers;
        this.responseMapper = responseMapper;
        this.contract = contract;
        this.credentials = credentials;
        this.passthrough = passthrough;
        this.defaultImageModel = defaultImageModel;
    }

    public Optional<Object> tryHandle(String rawBody, String authorization, String apiKey, String requestId,
            InboundRequestContext inbound, HttpServletResponse httpResponse) throws IOException {
        ResponsesImageRequest request = ResponsesImageRequest.parse(mapper, rawBody, defaultImageModel);
        if (request == null) return Optional.empty();
        InvokeGatewayCommand parent = requestMapper.toCommand(ContractBackedGatewayRequest.fromContract(
                contract.parseGatewayRequest(ProtocolType.OPENAI_RESPONSES, rawBody)), authorization, apiKey,
                requestId, ProtocolType.OPENAI_RESPONSES, inbound);
        credentials.authenticateForGateway(AuthenticateApiCredentialCommand.builder().keyHash(parent.getKeyHash())
                .requestedModel(parent.getRequestedCredentialModel()).build());
        // Reject invalid forced edits before committing SSE headers or contacting an upstream.
        ResponsesImagePayloadMapper.ImageRequest forcedImage = request.forced()
                ? payloadMapper.map(request, request.prompt(), null) : null;
        log.info("Responses image bridge accepted, requestId: {}, model: {}, imageModel: {}, forced: {}, streaming: {}",
                parent.getGatewayRequestId().value(), request.model(), request.imageModel(), request.forced(), request.streaming());
        if (request.streaming()) {
            httpResponse.setStatus(200);
            httpResponse.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
            httpResponse.setHeader("X-Accel-Buffering", "no");
            StreamingResponseBody body = stream -> {
                ResponsesImageResponseWriter writer = new ResponsesImageResponseWriter(mapper, request, stream);
                try {
                    writer.start();
                    execute(parent, request, forcedImage, writer);
                } catch (ResponsesImageBridgeFailure exception) {
                    writer.fail(errorBody(exception.response()));
                } catch (BusinessException exception) {
                    String type = List.of("TOKEN_QUOTA_EXHAUSTED", "MODEL_DAILY_LIMIT_EXCEEDED").contains(exception.code())
                            ? "rate_limit_error" : "invalid_request_error";
                    writer.fail(mapper.createObjectNode().put("type", type).put("code", exception.code()).put("message", exception.getMessage()));
                } catch (GatewayProtocolException exception) {
                    writer.fail(mapper.createObjectNode().put("type", exception.errorType()).put("message", exception.getMessage()));
                } catch (IllegalStateException exception) {
                    String code = exception.getMessage() == null ? "" : exception.getMessage().split(":", 2)[0];
                    if (!List.of("API_CREDENTIAL_DISABLED", "MODEL_NOT_ALLOWED", "TOKEN_QUOTA_EXHAUSTED",
                            "MODEL_DAILY_LIMIT_EXCEEDED").contains(code)) throw exception;
                    String type = code.contains("QUOTA") || code.contains("LIMIT") ? "rate_limit_error" : "invalid_request_error";
                    writer.fail(mapper.createObjectNode().put("type", type).put("code", code).put("message", exception.getMessage()));
                }
            };
            return Optional.of(body);
        }
        ResponsesImageResponseWriter writer = new ResponsesImageResponseWriter(mapper, request, null);
        try {
            execute(parent, request, forcedImage, writer);
            return Optional.of(GatewayRawResponse.of(writer.response().toString(), 200, MediaType.APPLICATION_JSON).toResponseEntity());
        } catch (ResponsesImageBridgeFailure exception) {
            return Optional.of(exception.response().toResponseEntity());
        }
    }

    private void execute(InvokeGatewayCommand parent, ResponsesImageRequest request,
            ResponsesImagePayloadMapper.ImageRequest forcedImage, ResponsesImageResponseWriter writer) throws IOException {
        if (forcedImage != null) {
            generate(parent, forcedImage, writer);
            writer.finish(null);
            return;
        }
        ObjectNode planned = successfulJson(child(parent, ProtocolType.OPENAI_RESPONSES,
                request.plannerRequest(mapper).toString(), ProtocolOperation.INVOKE));
        writer.addUsage(planned.path("usage"), false);
        if (!planned.path("output").isArray()) throw ResponsesImageBridgeFailure.invalidUpstream("Responses planner is missing output");
        long imageCalls = 0;
        for (JsonNode item : planned.path("output")) if (request.isImageCall(item)) imageCalls++;
        if (imageCalls > 0 && !request.imageAllowed()) {
            throw ResponsesImageBridgeFailure.invalidUpstream("Planner selected an image tool disallowed by tool_choice");
        }
        if (imageCalls > request.maxImageCalls()) throw ResponsesImageBridgeFailure.invalidUpstream("Too many image tool calls in a single response");
        for (JsonNode item : planned.path("output")) {
            if (!request.isImageCall(item)) {
                writer.appendItem(item);
                continue;
            }
            if (!"completed".equals(planned.path("status").asText("completed"))) {
                throw ResponsesImageBridgeFailure.invalidUpstream("Image tool selection did not complete");
            }
            ObjectNode arguments = readUpstreamObject(item.path("arguments").asText());
            ResponsesImagePayloadMapper.ImageRequest image = payloadMapper.map(request,
                    arguments.path("prompt").asText(), arguments.path("action").asText());
            generate(parent, image, writer);
        }
        writer.finish(planned);
    }

    private void generate(InvokeGatewayCommand parent, ResponsesImagePayloadMapper.ImageRequest image,
            ResponsesImageResponseWriter writer) throws IOException {
        InvokeGatewayCommand command = child(parent, ProtocolType.OPENAI_IMAGES, image.body(), image.operation());
        log.info("Responses image subrequest, parentRequestId: {}, requestId: {}, model: {}, operation: {}",
                parent.getGatewayRequestId().value(), command.getGatewayRequestId().value(),
                command.getRequestedModel().value(), image.operation());
        int index = writer.beginImage();
        if (command.isStreaming()) {
            streamImage(command, image, writer, index);
        } else {
            ObjectNode result = successfulJson(command);
            if (!result.path("data").isArray() || result.path("data").size() != 1) {
                throw ResponsesImageBridgeFailure.invalidUpstream("Images upstream must return one image for n=1");
            }
            writer.finishImage(index, result.path("data").get(0), image);
            writer.addUsage(result.path("usage"), true);
        }
    }

    private void streamImage(InvokeGatewayCommand command, ResponsesImagePayloadMapper.ImageRequest image,
            ResponsesImageResponseWriter writer, int index) throws IOException {
        GatewayStreamingInvocation invocation = gateway.openStreaming(command);
        if (!invocation.opened()) {
            throw new ResponsesImageBridgeFailure(responseMapper.toRawResponse(invocation.invocation(), invocation.upstreamMetadata()));
        }
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        try (ProviderStreamingResponse provider = invocation.providerResponse()) {
            ResponsesImageSseAdapter adapter = new ResponsesImageSseAdapter(mapper, writer, image, index);
            usage = passthrough.transferAndExtract(provider.body(), adapter, ProtocolType.OPENAI_IMAGES);
            // Some Images providers omit the optional SSE event: line, so account using terminal data too.
            adapter.finish();
            JsonNode imageUsage = adapter.completed().path("usage");
            if (!usage.usageKnown() && imageUsage.isObject()) {
                usage = UnifiedTokenUsage.known(imageUsage.path("input_tokens").asLong(), imageUsage.path("output_tokens").asLong(), 0, 0);
            }
            adapter.publish();
        } catch (ResponsesImageBridgeFailure exception) {
            gateway.completeStreamingFailure(invocation, exception);
            throw exception;
        } catch (IOException exception) {
            if (ClientDisconnectDetector.isClientDisconnect(exception)) {
                gateway.completeStreamingClientDisconnect(invocation, usage);
                throw exception;
            }
            gateway.completeStreamingFailure(invocation, new UncheckedIOException(exception));
            throw ResponsesImageBridgeFailure.invalidUpstream("Images upstream stream was interrupted");
        }
        gateway.completeStreamingSuccess(invocation, usage);
    }

    private ObjectNode successfulJson(InvokeGatewayCommand command) {
        GatewayRawResponse result = responseMapper.toRawResponse(gateway.invokeOutcome(command));
        if (result.statusCode() < 200 || result.statusCode() >= 300) throw new ResponsesImageBridgeFailure(result);
        return readUpstreamObject(result.body());
    }

    private ObjectNode readUpstreamObject(String json) {
        try {
            JsonNode value = mapper.readTree(json);
            if (value instanceof ObjectNode object) return object;
            throw ResponsesImageBridgeFailure.invalidUpstream("Expected an upstream JSON object");
        } catch (JsonProcessingException exception) {
            throw ResponsesImageBridgeFailure.invalidUpstream("Invalid upstream JSON response");
        }
    }

    private ObjectNode errorBody(GatewayRawResponse response) {
        try {
            JsonNode root = mapper.readTree(response.body());
            if (root != null && root.path("error").isObject()) return (ObjectNode) root.get("error");
        } catch (JsonProcessingException exception) {
            log.warn("Responses image bridge received a non-JSON error, status: {}", response.statusCode());
        }
        return mapper.createObjectNode().put("type", "api_error").put("message", "Upstream request failed (HTTP " + response.statusCode() + ")");
    }

    private InvokeGatewayCommand child(InvokeGatewayCommand parent, ProtocolType protocol, String body, ProtocolOperation operation) {
        var parsed = contract.parseGatewayRequest(protocol, body);
        // Inbound idempotency keys and response-protocol hints must not be reused for independent calls.
        Map<String, List<String>> headers = protocol == ProtocolType.OPENAI_IMAGES ? parent.getInbound().headers().entrySet().stream()
                .filter(entry -> HttpHeaders.USER_AGENT.equalsIgnoreCase(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)) : parent.getInbound().headers();
        return InvokeGatewayCommand.builder().gatewayInvocationId(identifiers.nextInvocationId())
                .gatewayRequestId(identifiers.requestId(null)).usageRecordId(identifiers.nextUsageRecordId())
                .keyHash(parent.getKeyHash())
                .requestedCredentialModel(com.api2api.domain.credential.model.ModelName.of(parsed.model()))
                .requestedModel(com.api2api.domain.channel.model.ModelName.of(parsed.model()))
                .requestProtocol(protocol).requestBody(body)
                .inbound(InboundRequestContext.of(headers, protocol == ProtocolType.OPENAI_IMAGES ? null : parent.getInbound().rawQuery(),
                        operation, parent.getInbound().clientIp()))
                .streaming(parsed.streaming()).toolCallingRequired(parsed.toolCallingRequired()).reasoningRequired(parsed.reasoningRequired()).build();
    }
}
