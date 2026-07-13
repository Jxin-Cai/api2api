package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * 在 streaming 直接透传（不需要协议转换）时，边透传 SSE 流边解析 usage 信息。
 */
@Component
public class StreamingPassthroughUsageExtractor {

    private final ObjectMapper objectMapper;

    public StreamingPassthroughUsageExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UnifiedTokenUsage transferAndExtract(
            InputStream input,
            OutputStream output,
            ProtocolType upstreamProtocol
    ) throws IOException {
        return switch (upstreamProtocol) {
            case CLAUDE_MESSAGES -> extractClaudeMessages(input, output);
            case OPENAI_RESPONSES -> extractOpenAIResponses(input, output);
            case OPENAI_CHAT_COMPLETIONS -> extractOpenAIChatCompletions(input, output);
            default -> {
                input.transferTo(output);
                yield UnifiedTokenUsage.unknown();
            }
        };
    }

    private UnifiedTokenUsage extractClaudeMessages(InputStream input, OutputStream output) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        String currentEvent = null;
        String line;
        while ((line = reader.readLine()) != null) {
            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
            } else if (line.startsWith("data:") && "message_delta".equals(currentEvent)) {
                String data = line.substring(5).trim();
                if (!data.isEmpty() && !"[DONE]".equals(data)) {
                    usage = tryExtractClaudeUsage(data, usage);
                }
            }
        }
        return usage;
    }

    private UnifiedTokenUsage extractOpenAIResponses(InputStream input, OutputStream output) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        String currentEvent = null;
        String line;
        while ((line = reader.readLine()) != null) {
            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
            } else if (line.startsWith("data:") && "response.completed".equals(currentEvent)) {
                String data = line.substring(5).trim();
                if (!data.isEmpty() && !"[DONE]".equals(data)) {
                    usage = tryExtractOpenAIResponsesUsage(data, usage);
                }
            }
        }
        return usage;
    }

    private UnifiedTokenUsage extractOpenAIChatCompletions(InputStream input, OutputStream output) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        String line;
        while ((line = reader.readLine()) != null) {
            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if (!data.isEmpty() && !"[DONE]".equals(data)) {
                    usage = tryExtractOpenAIChatCompletionsUsage(data, usage);
                }
            }
        }
        return usage;
    }

    private UnifiedTokenUsage tryExtractClaudeUsage(String data, UnifiedTokenUsage fallback) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode usageNode = node.path("usage");
            if (usageNode.isMissingNode() || usageNode.isNull()) {
                return fallback;
            }
            long outputTokens = usageNode.path("output_tokens").asLong(0);
            if (outputTokens <= 0) {
                return fallback;
            }
            long inputTokens = firstPositiveLong(usageNode.get("input_tokens"), usageNode.get("prompt_tokens"));
            long cacheCreation = firstPositiveLong(usageNode.get("cache_creation_input_tokens"));
            if (cacheCreation == 0) {
                cacheCreation = Math.max(0,
                        usageNode.path("cache_creation").path("ephemeral_5m_input_tokens").asLong(0)
                                + usageNode.path("cache_creation").path("ephemeral_1h_input_tokens").asLong(0));
            }
            long cacheRead = firstPositiveLong(
                    usageNode.get("cache_read_input_tokens"),
                    usageNode.get("cached_tokens")
            );
            return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheCreation, cacheRead);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private UnifiedTokenUsage tryExtractOpenAIResponsesUsage(String data, UnifiedTokenUsage fallback) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode response = node.path("response");
            JsonNode usageNode = response.isMissingNode() ? node.path("usage") : response.path("usage");
            if (usageNode.isMissingNode() || usageNode.isNull()) {
                return fallback;
            }
            return OpenAIResponsesUsageExtractor.extractUsage(usageNode);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private UnifiedTokenUsage tryExtractOpenAIChatCompletionsUsage(String data, UnifiedTokenUsage fallback) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode usageNode = node.path("usage");
            if (usageNode.isMissingNode() || usageNode.isNull()) {
                return fallback;
            }
            long cacheReadTokens = usageNode.path("prompt_tokens_details").path("cached_tokens").asLong(0);
            long promptTokens = usageNode.path("prompt_tokens").asLong(0);
            long inputTokens = Math.max(0, promptTokens - cacheReadTokens);
            long outputTokens = usageNode.path("completion_tokens").asLong(0);
            if (inputTokens <= 0 && outputTokens <= 0) {
                return fallback;
            }
            return UnifiedTokenUsage.known(inputTokens, outputTokens, 0, cacheReadTokens);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long firstPositiveLong(JsonNode... values) {
        for (JsonNode value : values) {
            if (value != null && !value.isNull() && !value.isMissingNode() && value.asLong(0) > 0) {
                return value.asLong();
            }
        }
        return 0;
    }
}
