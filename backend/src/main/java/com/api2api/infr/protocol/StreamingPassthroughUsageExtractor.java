package com.api2api.infr.protocol;

import com.api2api.application.gateway.StreamingPassthroughPort;
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
import java.util.Set;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 在 streaming 直接透传（不需要协议转换）时，边透传 SSE 流边解析 usage 信息。
 */
@Component
public class StreamingPassthroughUsageExtractor implements StreamingPassthroughPort {

    private static final Logger log = LoggerFactory.getLogger(StreamingPassthroughUsageExtractor.class);
    private static final Set<String> CLAUDE_TERMINAL_EVENTS = Set.of("message_stop", "error");
    private static final Set<String> RESPONSES_TERMINAL_EVENTS = Set.of(
            "response.completed",
            "response.failed",
            "response.incomplete",
            "error"
    );
    private static final Set<String> IMAGES_USAGE_EVENTS = Set.of(
            "image_generation.completed",
            "image_edit.completed"
    );
    private static final Set<String> IMAGES_TERMINAL_EVENTS = Set.of(
            "image_generation.completed",
            "image_edit.completed",
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
            case OPENAI_IMAGES -> extractOpenAIImages(input, output);
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
                Set.of("message_delta"),
                CLAUDE_TERMINAL_EVENTS,
                this::tryExtractClaudeUsage
        );
    }

    private UnifiedTokenUsage extractOpenAIResponses(InputStream input, OutputStream output) throws IOException {
        return extractByEvent(
                input,
                output,
                Set.of("response.completed"),
                RESPONSES_TERMINAL_EVENTS,
                this::tryExtractOpenAIResponsesUsage
        );
    }

    /**
     * Generations and edits streams end with differently named completed events; both carry usage.
     */
    private UnifiedTokenUsage extractOpenAIImages(InputStream input, OutputStream output) throws IOException {
        return extractByEvent(
                input,
                output,
                IMAGES_USAGE_EVENTS,
                IMAGES_TERMINAL_EVENTS,
                this::tryExtractOpenAIImagesUsage
        );
    }

    private UnifiedTokenUsage extractByEvent(
            InputStream input,
            OutputStream output,
            Set<String> usageEvents,
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
                terminalEventSeen |= terminalEvents.contains(currentEvent);
            } else if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                terminalEventSeen |= isTerminalData(data, terminalEvents);
                if (currentEvent != null && usageEvents.contains(currentEvent)
                        && !data.isEmpty() && !"[DONE]".equals(data)) {
                    usage = extractor.apply(data, usage);
                }
            } else if (line.isEmpty()) {
                output.flush();
                currentEvent = null;
            }
        }
        output.flush();
        warnWhenStreamEndedWithoutTerminalEvent(terminalEventSeen);
        return usage;
    }

    /**
     * Recognises a terminal event carried only in the data payload, for upstreams that omit the
     * optional {@code event:} line. SSE permits it, and treating those streams as truncated would
     * abort otherwise complete responses.
     */
    private boolean isTerminalData(String data, Set<String> terminalEvents) {
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return false;
        }
        try {
            return terminalEvents.contains(objectMapper.readTree(data).path("type").asText(""));
        } catch (com.fasterxml.jackson.core.JsonProcessingException notJson) {
            return false;
        }
    }

    private UnifiedTokenUsage extractOpenAIChatCompletions(InputStream input, OutputStream output) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        boolean terminalEventSeen = false;
        String line;
        while ((line = reader.readLine()) != null) {
            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    terminalEventSeen = true;
                } else if (!data.isEmpty()) {
                    usage = tryExtractOpenAIChatCompletionsUsage(data, usage);
                }
            }
            if (line.isEmpty()) {
                output.flush();
            }
        }
        output.flush();
        warnWhenStreamEndedWithoutTerminalEvent(terminalEventSeen);
        return usage;
    }

    /**
     * A missing terminal event is reported but not escalated. The upstream body was relayed to the
     * client byte for byte, so injecting a synthetic error event here would turn a complete response
     * into a client-visible failure whenever a provider closes the connection without a trailer.
     */
    private void warnWhenStreamEndedWithoutTerminalEvent(boolean terminalEventSeen) {
        if (!terminalEventSeen) {
            log.warn("Upstream SSE stream ended without a terminal event; relayed body may be truncated");
        }
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
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException expected) {
            return fallback;
        } catch (Exception unexpected) {
            log.warn("Unexpected error extracting usage from streaming data", unexpected);
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
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException expected) {
            return fallback;
        } catch (Exception unexpected) {
            log.warn("Unexpected error extracting usage from streaming data", unexpected);
            return fallback;
        }
    }

    private UnifiedTokenUsage tryExtractOpenAIImagesUsage(String data, UnifiedTokenUsage fallback) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode usageNode = node.path("usage");
            if (usageNode.isMissingNode() || usageNode.isNull()) {
                return fallback;
            }
            return OpenAIImagesUsageExtractor.extractUsage(usageNode);
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException expected) {
            return fallback;
        } catch (Exception unexpected) {
            log.warn("Unexpected error extracting usage from streaming data", unexpected);
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
            UnifiedTokenUsage usage = OpenAIChatCompletionsUsageExtractor.extractUsage(usageNode);
            return (usage.inputTokens() <= 0 && usage.outputTokens() <= 0) ? fallback : usage;
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException expected) {
            return fallback;
        } catch (Exception unexpected) {
            log.warn("Unexpected error extracting usage from streaming data", unexpected);
            return fallback;
        }
    }

}
