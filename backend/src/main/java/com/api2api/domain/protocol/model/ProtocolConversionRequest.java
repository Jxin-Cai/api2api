package com.api2api.domain.protocol.model;

/**
 * 本次调用对协议转换能力的需求。
 */
public final class ProtocolConversionRequest {
    private final boolean streaming;
    private final boolean toolCallingRequired;
    private final boolean reasoningRequired;

    private ProtocolConversionRequest(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        this.streaming = streaming;
        this.toolCallingRequired = toolCallingRequired;
        this.reasoningRequired = reasoningRequired;
    }

    public static ProtocolConversionRequest of(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        return new ProtocolConversionRequest(streaming, toolCallingRequired, reasoningRequired);
    }

    public boolean streaming() {
        return streaming;
    }

    public boolean toolCallingRequired() {
        return toolCallingRequired;
    }

    public boolean reasoningRequired() {
        return reasoningRequired;
    }
}
