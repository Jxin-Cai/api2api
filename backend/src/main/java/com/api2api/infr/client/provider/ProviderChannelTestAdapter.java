package com.api2api.infr.client.provider;

import com.api2api.application.BusinessException;
import com.api2api.application.channel.ProviderChannelTestPort;
import com.api2api.application.channel.dto.ChannelTestResult;
import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.application.gateway.UpstreamGatewayException;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.api2api.domain.protocol.repository.ProtocolConversionDefinitionRepository;
import com.api2api.domain.protocol.service.ProtocolConversionService;
import com.api2api.domain.routing.model.RouteFailureType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProviderChannelTestAdapter implements ProviderChannelTestPort {

    private static final String HELLO = "hello";
    private static final int MAX_REPLY_LENGTH = 500;
    private final ObjectMapper json;
    private final ProtocolConversionDefinitionRepository definitions;
    private final ProtocolConversionService conversion;
    private final BearerTokenProviderCallStrategy transport;

    @Override
    public ChannelTestResult sendHello(ProviderChannel channel, ProtocolType requestProtocol, ChannelModelSupport model) {
        var routes = definitions.findAll();
        var requirement = ProtocolConversionRequest.of(false, false, false)
                .forRoute(channel.id().value(), model.upstreamModel().value());
        ProtocolType upstream = model.upstreamProtocol();
        long start = System.nanoTime();
        try {
            ProtocolPayload hello = ProtocolPayload.of(requestProtocol, helloBody(requestProtocol, model), false);
            var request = conversion.convertRequest(hello, upstream, requirement, routes);
            ProviderGatewayResponse response = transport.sendTest(channel, model, request.body());
            if (!response.successful()) {
                throw httpFailure(response.statusCode());
            }
            String reply = readReply(upstream, response.body());
            if (upstream != requestProtocol) {
                var converted = conversion.convertResponse(ProtocolPayload.of(upstream, response.body(), false),
                        requestProtocol, requirement, routes);
                reply = readReply(requestProtocol, converted.body());
            }
            String preview = reply.replace(channel.keyRef().value(), "***").strip();
            if (preview.length() > MAX_REPLY_LENGTH) {
                preview = preview.substring(0, MAX_REPLY_LENGTH) + "…";
            }
            return new ChannelTestResult(response.statusCode(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), preview);
        } catch (ProtocolConversionException ex) {
            throw new BusinessException("CHANNEL_TEST_CONVERSION_FAILED", "协议转换失败，请检查该方向的转换配置", ex);
        } catch (UpstreamGatewayException ex) {
            String reason = ex.failureType() == RouteFailureType.TIMEOUT ? "请求上游超时" : "无法连接上游，请检查渠道地址和网络";
            throw new BusinessException("CHANNEL_TEST_CONNECTION_FAILED", reason, ex);
        }
    }

    private String helloBody(ProtocolType protocol, ChannelModelSupport model) {
        ObjectNode body = json.createObjectNode().put("model", model.upstreamModel().value());
        switch (protocol) {
            case OPENAI_CHAT_COMPLETIONS, CLAUDE_MESSAGES -> {
                body.putArray("messages").addObject().put("role", "user").put("content", HELLO);
                if (protocol == ProtocolType.CLAUDE_MESSAGES) {
                    body.put("max_tokens", 128);
                }
            }
            case OPENAI_RESPONSES -> body.put("input", HELLO);
            case OPENAI_IMAGES -> body.put("prompt", HELLO).put("n", 1);
            case AWS_BEDROCK_CLAUDE_MESSAGES -> throw new BusinessException("CHANNEL_TEST_INVALID_PROTOCOL",
                    "请选择客户端请求协议，Bedrock 仅支持作为上游协议");
        }
        return body.toString();
    }

    private String readReply(ProtocolType protocol, String body) {
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("CHANNEL_TEST_INVALID_RESPONSE", "上游返回了无效的 JSON 响应", ex);
        }
        if (root == null || !root.isObject() || root.hasNonNull("error") || "failed".equals(root.path("status").asText())) {
            throw new BusinessException("CHANNEL_TEST_INVALID_RESPONSE", "上游未返回成功的模型响应");
        }
        String reply = switch (protocol) {
            case OPENAI_CHAT_COMPLETIONS -> {
                JsonNode content = root.at("/choices/0/message/content");
                yield content.isTextual() ? content.textValue() : blockText(content, "text");
            }
            case CLAUDE_MESSAGES, AWS_BEDROCK_CLAUDE_MESSAGES -> blockText(root.path("content"), "text");
            case OPENAI_RESPONSES -> responsesText(root);
            case OPENAI_IMAGES -> {
                JsonNode image = root.at("/data/0");
                yield hasText(image.path("url")) || hasText(image.path("b64_json")) ? "上游已返回 1 张图片" : "";
            }
        };
        if (reply.isBlank()) {
            throw new BusinessException("CHANNEL_TEST_EMPTY_REPLY", "上游响应中没有该协议的模型输出");
        }
        return reply;
    }

    private static String responsesText(JsonNode root) {
        StringBuilder text = new StringBuilder();
        for (JsonNode item : root.path("output")) {
            if ("message".equals(item.path("type").asText())) {
                text.append(blockText(item.path("content"), "output_text"));
            }
        }
        return text.isEmpty() && hasText(root.path("output_text")) ? root.path("output_text").textValue() : text.toString();
    }

    private static String blockText(JsonNode blocks, String type) {
        StringBuilder text = new StringBuilder();
        for (JsonNode block : blocks) {
            if (type.equals(block.path("type").asText()) && hasText(block.path("text"))) {
                text.append(block.path("text").textValue());
            }
        }
        return text.toString();
    }

    private static boolean hasText(JsonNode node) {
        return node.isTextual() && !node.textValue().isBlank();
    }

    private static BusinessException httpFailure(int status) {
        String message = switch (status) {
            case 401, 403 -> "上游拒绝访问，请检查渠道密钥和模型权限";
            case 404 -> "上游找不到接口或模型";
            case 429 -> "上游限流或额度不足";
            default -> "上游调用失败";
        };
        return new BusinessException("CHANNEL_TEST_UPSTREAM_ERROR", message + "（HTTP " + status + "）");
    }
}
