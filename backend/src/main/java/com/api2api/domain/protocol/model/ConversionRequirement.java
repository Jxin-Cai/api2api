package com.api2api.domain.protocol.model;

/**
 * 设计文档中的转换需求值对象，语义等同于 ProtocolConversionRequest。
 */
public final class ConversionRequirement {
    private final boolean streaming;
    private final boolean toolCallingRequired;
    private final boolean reasoningRequired;

    private ConversionRequirement(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        this.streaming = streaming;
        this.toolCallingRequired = toolCallingRequired;
        this.reasoningRequired = reasoningRequired;
    }

    public static ConversionRequirement of(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        return new ConversionRequirement(streaming, toolCallingRequired, reasoningRequired);
    }

    public ProtocolConversionRequest toProtocolConversionRequest() {
        return ProtocolConversionRequest.of(streaming, toolCallingRequired, reasoningRequired);
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
