package com.api2api.infr.client.provider;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for upstream provider HTTP clients.
 */
@ConfigurationProperties(prefix = "api2api.provider-http")
public class ProviderHttpClientProperties {

    private static final Duration DEFAULT_UPSTREAM_READ_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration DEFAULT_STREAMING_FIRST_BYTE_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration DEFAULT_STREAMING_IDLE_TIMEOUT = Duration.ofMinutes(10);

    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration modelsReadTimeout = Duration.ofSeconds(10);
    private int modelsMaxRetries = 1;
    private boolean allowInsecureHosts = false;
    private Duration upstreamReadTimeout = DEFAULT_UPSTREAM_READ_TIMEOUT;
    private Duration streamingFirstByteTimeout = DEFAULT_STREAMING_FIRST_BYTE_TIMEOUT;
    private Duration streamingIdleTimeout = DEFAULT_STREAMING_IDLE_TIMEOUT;
    private int streamingMaxRetries = 2;
    private Duration streamingRetryBackoff = Duration.ofMillis(200);
    private String upstreamHostOverride = "";
    private String modelsPath = "/models";
    private String claudeMessagesPath = "/v1/messages";
    private String openaiResponsesPath = "/v1/responses";
    private String openaiChatCompletionsPath = "/v1/chat/completions";
    private String bedrockConversePathTemplate = "/model/{modelId}/converse";
    private String bedrockConverseStreamPathTemplate = "/model/{modelId}/converse-stream";
    private String anthropicVersion = "2023-06-01";
    private Set<String> passthroughHeaderAllowlist = new LinkedHashSet<>(Set.of(
            "x-request-id",
            "x-correlation-id",
            "anthropic-beta",
            "openai-beta",
            "traceparent",
            "tracestate",
            "baggage"
    ));
    private Set<String> headerDenylist = new LinkedHashSet<>(Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "host",
            "connection",
            "content-length",
            "transfer-encoding",
            "x-api-key"
    ));

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = positiveDuration(connectTimeout, "Connect timeout must be positive");
    }

    public Duration getModelsReadTimeout() {
        return modelsReadTimeout;
    }

    public void setModelsReadTimeout(Duration modelsReadTimeout) {
        this.modelsReadTimeout = positiveDuration(modelsReadTimeout, "Models read timeout must be positive");
    }

    public int getModelsMaxRetries() {
        return modelsMaxRetries;
    }

    public boolean isAllowInsecureHosts() {
        return allowInsecureHosts;
    }

    public void setAllowInsecureHosts(boolean allowInsecureHosts) {
        this.allowInsecureHosts = allowInsecureHosts;
    }

    public void setModelsMaxRetries(int modelsMaxRetries) {
        if (modelsMaxRetries < 0) {
            throw new IllegalArgumentException("Models max retries must not be negative");
        }
        this.modelsMaxRetries = modelsMaxRetries;
    }

    public Duration getUpstreamReadTimeout() {
        return upstreamReadTimeout;
    }

    public void setUpstreamReadTimeout(Duration upstreamReadTimeout) {
        this.upstreamReadTimeout = positiveDuration(upstreamReadTimeout, "Upstream read timeout must be positive");
    }

    public Duration getStreamingFirstByteTimeout() {
        return streamingFirstByteTimeout;
    }

    public void setStreamingFirstByteTimeout(Duration streamingFirstByteTimeout) {
        this.streamingFirstByteTimeout = positiveDuration(streamingFirstByteTimeout, "Streaming first byte timeout must be positive");
    }

    public Duration getStreamingIdleTimeout() {
        return streamingIdleTimeout;
    }

    public void setStreamingIdleTimeout(Duration streamingIdleTimeout) {
        this.streamingIdleTimeout = positiveDuration(streamingIdleTimeout, "Streaming idle timeout must be positive");
    }

    public int getStreamingMaxRetries() {
        return streamingMaxRetries;
    }

    public void setStreamingMaxRetries(int streamingMaxRetries) {
        if (streamingMaxRetries < 0) {
            throw new IllegalArgumentException("Streaming max retries must not be negative");
        }
        this.streamingMaxRetries = streamingMaxRetries;
    }

    public Duration getStreamingRetryBackoff() {
        return streamingRetryBackoff;
    }

    public void setStreamingRetryBackoff(Duration streamingRetryBackoff) {
        this.streamingRetryBackoff = positiveDuration(streamingRetryBackoff, "Streaming retry backoff must be positive");
    }

    public String getUpstreamHostOverride() {
        return upstreamHostOverride;
    }

    public void setUpstreamHostOverride(String upstreamHostOverride) {
        this.upstreamHostOverride = upstreamHostOverride == null ? "" : upstreamHostOverride.trim();
    }

    public String getModelsPath() {
        return modelsPath;
    }

    public void setModelsPath(String modelsPath) {
        this.modelsPath = normalizePath(modelsPath, "Models path must start with /");
    }

    public String getClaudeMessagesPath() {
        return claudeMessagesPath;
    }

    public void setClaudeMessagesPath(String claudeMessagesPath) {
        this.claudeMessagesPath = normalizePath(claudeMessagesPath, "Claude messages path must start with /");
    }

    public String getOpenaiResponsesPath() {
        return openaiResponsesPath;
    }

    public void setOpenaiResponsesPath(String openaiResponsesPath) {
        this.openaiResponsesPath = normalizePath(openaiResponsesPath, "OpenAI responses path must start with /");
    }

    public String getOpenaiChatCompletionsPath() {
        return openaiChatCompletionsPath;
    }

    public void setOpenaiChatCompletionsPath(String openaiChatCompletionsPath) {
        this.openaiChatCompletionsPath = normalizePath(openaiChatCompletionsPath, "OpenAI chat completions path must start with /");
    }

    public String getBedrockConversePathTemplate() {
        return bedrockConversePathTemplate;
    }

    public void setBedrockConversePathTemplate(String bedrockConversePathTemplate) {
        this.bedrockConversePathTemplate = normalizePathTemplate(
                bedrockConversePathTemplate,
                "Bedrock Converse path template must start with /"
        );
    }

    public String getBedrockConverseStreamPathTemplate() {
        return bedrockConverseStreamPathTemplate;
    }

    public void setBedrockConverseStreamPathTemplate(String bedrockConverseStreamPathTemplate) {
        this.bedrockConverseStreamPathTemplate = normalizePathTemplate(
                bedrockConverseStreamPathTemplate,
                "Bedrock Converse stream path template must start with /"
        );
    }

    public String getAnthropicVersion() {
        return anthropicVersion;
    }

    public void setAnthropicVersion(String anthropicVersion) {
        if (anthropicVersion == null || anthropicVersion.isBlank()) {
            throw new IllegalArgumentException("Anthropic version must not be blank");
        }
        this.anthropicVersion = anthropicVersion.trim();
    }

    public Set<String> getPassthroughHeaderAllowlist() {
        return Set.copyOf(passthroughHeaderAllowlist);
    }

    public void setPassthroughHeaderAllowlist(Set<String> passthroughHeaderAllowlist) {
        this.passthroughHeaderAllowlist = normalizeHeaderNames(passthroughHeaderAllowlist);
    }

    public Set<String> getHeaderDenylist() {
        return Set.copyOf(headerDenylist);
    }

    public void setHeaderDenylist(Set<String> headerDenylist) {
        this.headerDenylist = normalizeHeaderNames(headerDenylist);
    }

    public String defaultPathFor(com.api2api.domain.channel.model.ProtocolType protocolType) {
        Objects.requireNonNull(protocolType, "Protocol type must not be null");
        return switch (protocolType) {
            case CLAUDE_MESSAGES -> claudeMessagesPath;
            case OPENAI_RESPONSES -> openaiResponsesPath;
            case OPENAI_CHAT_COMPLETIONS -> openaiChatCompletionsPath;
            case AWS_BEDROCK_CONVERSE -> null;
        };
    }

    private static Duration positiveDuration(Duration duration, String message) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(message);
        }
        return duration;
    }

    private static String normalizePath(String path, String message) {
        if (path == null || path.isBlank() || !path.trim().startsWith("/")) {
            throw new IllegalArgumentException(message);
        }
        return path.trim();
    }

    private static String normalizePathTemplate(String pathTemplate, String message) {
        String normalized = normalizePath(pathTemplate, message);
        if (!normalized.contains("{modelId}")) {
            throw new IllegalArgumentException("Bedrock path template must contain {modelId}");
        }
        return normalized;
    }

    private static Set<String> normalizeHeaderNames(Set<String> names) {
        Set<String> normalized = new LinkedHashSet<>();
        if (names == null) {
            return normalized;
        }
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                normalized.add(name.trim().toLowerCase());
            }
        }
        return normalized;
    }
}
