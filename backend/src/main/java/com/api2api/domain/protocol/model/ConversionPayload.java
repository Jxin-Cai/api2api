package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;

/**
 * 设计文档中的转换输入载体别名，语义等同于 ProtocolPayload。
 */
public final class ConversionPayload {
    private final ProtocolPayload payload;

    private ConversionPayload(ProtocolPayload payload) {
        this.payload = payload;
    }

    public static ConversionPayload of(ProtocolType protocol, String body, boolean streaming) {
        return new ConversionPayload(ProtocolPayload.of(protocol, body, streaming));
    }

    public static ConversionPayload from(ProtocolPayload payload) {
        return new ConversionPayload(payload);
    }

    public ProtocolPayload toProtocolPayload() {
        return payload;
    }

    public ProtocolType protocol() {
        return payload.protocol();
    }

    public String body() {
        return payload.body();
    }

    public boolean streaming() {
        return payload.streaming();
    }
}
