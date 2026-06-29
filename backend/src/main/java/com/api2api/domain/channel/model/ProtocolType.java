package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * MVP 支持的上游/客户端协议类型。
 */
public enum ProtocolType {
    CLAUDE_MESSAGES("/v1/messages"),
    OPENAI_RESPONSES("/v1/responses"),
    OPENAI_CHAT_COMPLETIONS("/v1/chat/completions");

    private final String defaultEndpointPath;

    ProtocolType(String defaultEndpointPath) {
        this.defaultEndpointPath = defaultEndpointPath;
    }

    public boolean isSameAs(ProtocolType other) {
        return this == Objects.requireNonNull(other, "protocol must not be null");
    }

    public String defaultEndpointPath() {
        return defaultEndpointPath;
    }
}
