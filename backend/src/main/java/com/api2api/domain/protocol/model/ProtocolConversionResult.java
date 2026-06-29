package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;
import java.util.Optional;

/**
 * 协议转换输出结果。
 */
public final class ProtocolConversionResult {
    private final ProtocolType sourceProtocol;
    private final ProtocolType targetProtocol;
    private final String body;
    private final boolean passthrough;
    private final UnifiedTokenUsage usage;

    private ProtocolConversionResult(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            String body,
            boolean passthrough,
            UnifiedTokenUsage usage
    ) {
        this.sourceProtocol = Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null");
        this.targetProtocol = Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
        if (body == null || body.trim().isEmpty()) {
            throw new ProtocolConversionException("conversion result body must not be blank");
        }
        boolean expectedPassthrough = sourceProtocol == targetProtocol;
        if (passthrough != expectedPassthrough) {
            throw new ProtocolConversionException("passthrough flag must match source and target protocol relation");
        }
        this.body = body;
        this.passthrough = passthrough;
        this.usage = usage;
    }

    public static ProtocolConversionResult of(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            String body,
            boolean passthrough,
            UnifiedTokenUsage usage
    ) {
        return new ProtocolConversionResult(sourceProtocol, targetProtocol, body, passthrough, usage);
    }

    public static ProtocolConversionResult passthrough(ProtocolPayload payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        return new ProtocolConversionResult(payload.protocol(), payload.protocol(), payload.body(), true, null);
    }

    public ProtocolType sourceProtocol() {
        return sourceProtocol;
    }

    public ProtocolType targetProtocol() {
        return targetProtocol;
    }

    public String body() {
        return body;
    }

    public boolean passthrough() {
        return passthrough;
    }

    public Optional<UnifiedTokenUsage> usage() {
        return Optional.ofNullable(usage);
    }
}
