package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;

/**
 * 协议转换输入载体，承载 JSON 或 SSE 事件片段。
 */
public final class ProtocolPayload {
    private final ProtocolType protocol;
    private final String body;
    private final boolean streaming;

    private ProtocolPayload(ProtocolType protocol, String body, boolean streaming) {
        this.protocol = Objects.requireNonNull(protocol, "protocol must not be null");
        if (body == null || body.trim().isEmpty()) {
            throw new ProtocolConversionException("payload body must not be blank");
        }
        this.body = body;
        this.streaming = streaming;
    }

    public static ProtocolPayload of(ProtocolType protocol, String body, boolean streaming) {
        return new ProtocolPayload(protocol, body, streaming);
    }

    public ProtocolType protocol() {
        return protocol;
    }

    public String body() {
        return body;
    }

    public boolean streaming() {
        return streaming;
    }
}
