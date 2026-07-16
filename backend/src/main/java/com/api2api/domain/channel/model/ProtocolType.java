package com.api2api.domain.channel.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * MVP 支持的上游/客户端协议类型。
 */
public enum ProtocolType {
    CLAUDE_MESSAGES("/v1/messages"),
    OPENAI_RESPONSES("/v1/responses"),
    OPENAI_CHAT_COMPLETIONS("/v1/chat/completions"),
    AWS_BEDROCK_CONVERSE("/model/{modelId}/converse"),
    AWS_BEDROCK_CLAUDE_MESSAGES("/model/{modelId}/invoke");

    private final String defaultEndpointPath;

    ProtocolType(String defaultEndpointPath) {
        this.defaultEndpointPath = defaultEndpointPath;
    }

    public boolean isSameAs(ProtocolType other) {
        return this == Objects.requireNonNull(other, "protocol must not be null");
    }

    public boolean isClientFacing() {
        return this != AWS_BEDROCK_CONVERSE && this != AWS_BEDROCK_CLAUDE_MESSAGES;
    }

    public String defaultEndpointPath() {
        return defaultEndpointPath;
    }

    public static Optional<ProtocolType> parseExternal(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        for (ProtocolType protocolType : values()) {
            if (protocolType.name().equalsIgnoreCase(trimmed)
                    || protocolType.defaultEndpointPath.equalsIgnoreCase(trimmed)) {
                return Optional.of(protocolType);
            }
        }
        String normalized = trimmed
                .toUpperCase(Locale.ROOT)
                .replace("/V1/", "")
                .replace("/", "_")
                .replace("-", "_")
                .replace(" ", "_");
        if (normalized.startsWith("V1_")) {
            normalized = normalized.substring(3);
        }
        if ("MESSAGES".equals(normalized)) {
            normalized = "CLAUDE_MESSAGES";
        } else if ("RESPONSES".equals(normalized)) {
            normalized = "OPENAI_RESPONSES";
        } else if ("CHAT_COMPLETIONS".equals(normalized) || "OPENAI_CHAT".equals(normalized)) {
            normalized = "OPENAI_CHAT_COMPLETIONS";
        } else if ("BEDROCK_CONVERSE".equals(normalized)) {
            normalized = "AWS_BEDROCK_CONVERSE";
        } else if ("BEDROCK_CLAUDE_MESSAGES".equals(normalized)
                || "BEDROCK_INVOKE".equals(normalized)
                || "AWS_BEDROCK_INVOKE".equals(normalized)) {
            normalized = "AWS_BEDROCK_CLAUDE_MESSAGES";
        }
        try {
            return Optional.of(ProtocolType.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
