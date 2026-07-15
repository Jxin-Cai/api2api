package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

/**
 * 在 streaming 直接透传（不需要协议转换）时，边透传 SSE 流边解析 usage 信息。
 */
@Component
public class StreamingPassthroughUsageExtractor {

    private static final Set<String> CLAUDE_TERMINAL_EVENTS = Set.of("message_stop", "error");
    private static final Set<String> RESPONSES_TERMINAL_EVENTS = Set.of(
            "response.completed",
            "response.failed",
            "response.incomplete",
            "error"
    );

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
        return extractByEvent(
                input,
                output,
                "message_delta",
                CLAUDE_TERMINAL_EVENTS,
                this::tryExtractClaudeUsage
        );
    }

    private UnifiedTokenUsage extractOpenAIResponses(InputStream input, OutputStream output) throws IOException {
        return extractByEvent(
                input,
                output,
                "response.completed",
                RESPONSES_TERMINAL_EVENTS,
                this::tryExtractOpenAIResponsesUsage
        );
    }

    private UnifiedTokenUsage extractByEvent(
            InputStream input,
            OutputStream output,
            String targetEvent,
            Set<String> terminalEvents,
            BiFunction<String, UnifiedTokenUsage, UnifiedTokenUsage> extractor
    ) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        String currentEvent = null;
        boolean terminalEventSeen = false;
        String line;
        while ((line = reader.readLine()) != null) {
            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
            } else if (line.startsWith("data:") && targetEvent.equals(currentEvent)) {
                String data = line.substring(5).trim();
                if (!data.isEmpty() && !"[DONE]".equals(data)) {
                    usage = extractor.apply(data, usage);
                }
            } else if (line.isEmpty()) {
                terminalEventSeen |= terminalEvents.contains(currentEvent);
                output.flush();
                currentEvent = null;
            }
        }
        output.flush();
        if (!terminalEventSeen) {
            throw new EOFException("Upstream SSE stream ended before a terminal event");
        }
        return usage;
    }

    private UnifiedTokenUsage extractOpenAIChatCompletions(InputStream input, OutputStream output) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        boolean terminalEventSeen = false;
        boolean currentEventTerminal = false;
        String line;
        while ((line = reader.readLine()) != null) {
            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    currentEventTerminal = true;
                } else if (!data.isEmpty()) {
                    usage = tryExtractOpenAIChatCompletionsUsage(data, usage);
                }
            }
            if (line.isEmpty()) {
                terminalEventSeen |= currentEventTerminal;
                currentEventTerminal = false;
                output.flush();
            }
        }
        output.flush();
        if (!terminalEventSeen) {
            throw new EOFException("Upstream SSE stream ended before a terminal event");
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
            long inputTokens = ClaudeMessagesUsageExtractor.firstPositiveLong(usageNode.get("input_tokens"), usageNode.get("prompt_tokens"));
            long cacheCreation = ClaudeMessagesUsageExtractor.firstPositiveLong(usageNode.get("cache_creation_input_tokens"));
            if (cacheCreation == 0) {
                cacheCreation = Math.max(0,
                        usageNode.path("cache_creation").path("ephemeral_5m_input_tokens").asLong(0)
                                + usageNode.path("cache_creation").path("ephemeral_1h_input_tokens").asLong(0));
            }
            long cacheRead = ClaudeMessagesUsageExtractor.firstPositiveLong(
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
            JsonNode details = usageNode.path("prompt_tokens_details");
            long cacheReadTokens = details.path("cached_tokens").asLong(0);
            long cacheWriteTokens = details.path("cache_write_tokens").asLong(0);
            long promptTokens = usageNode.path("prompt_tokens").asLong(0);
            long inputTokens = Math.max(0, promptTokens - cacheReadTokens - cacheWriteTokens);
            long outputTokens = usageNode.path("completion_tokens").asLong(0);
            if (inputTokens <= 0 && outputTokens <= 0) {
                return fallback;
            }
            return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheWriteTokens, cacheReadTokens);
        } catch (Exception ignored) {
            return fallback;
        }
    }

}
