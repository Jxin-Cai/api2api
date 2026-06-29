package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Optional;

/**
 * 设计文档中的转换结果别名，语义等同于 ProtocolConversionResult。
 */
public final class ConversionResult {
    private final ProtocolConversionResult result;

    private ConversionResult(ProtocolConversionResult result) {
        this.result = result;
    }

    public static ConversionResult of(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            String body,
            boolean passthrough,
            UnifiedTokenUsage usage
    ) {
        return new ConversionResult(ProtocolConversionResult.of(sourceProtocol, targetProtocol, body, passthrough, usage));
    }

    public static ConversionResult from(ProtocolConversionResult result) {
        return new ConversionResult(result);
    }

    public ProtocolConversionResult toProtocolConversionResult() {
        return result;
    }

    public ProtocolType sourceProtocol() {
        return result.sourceProtocol();
    }

    public ProtocolType targetProtocol() {
        return result.targetProtocol();
    }

    public String body() {
        return result.body();
    }

    public boolean passthrough() {
        return result.passthrough();
    }

    public Optional<UnifiedTokenUsage> usage() {
        return result.usage();
    }
}
