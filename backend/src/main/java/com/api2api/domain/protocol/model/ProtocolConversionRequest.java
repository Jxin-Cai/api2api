package com.api2api.domain.protocol.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 本次调用对协议转换能力的需求。
 */
public final class ProtocolConversionRequest {
    private final boolean streaming;
    private final boolean toolCallingRequired;
    private final boolean reasoningRequired;
    private final ProtocolConversionRouteContext routeContext;
    private final List<String> anthropicBetaFeatures;

    private ProtocolConversionRequest(
            boolean streaming,
            boolean toolCallingRequired,
            boolean reasoningRequired,
            ProtocolConversionRouteContext routeContext,
            List<String> anthropicBetaFeatures
    ) {
        this.streaming = streaming;
        this.toolCallingRequired = toolCallingRequired;
        this.reasoningRequired = reasoningRequired;
        this.routeContext = routeContext;
        this.anthropicBetaFeatures = List.copyOf(anthropicBetaFeatures);
    }

    public static ProtocolConversionRequest of(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        return new ProtocolConversionRequest(streaming, toolCallingRequired, reasoningRequired, null, List.of());
    }

    public ProtocolConversionRequest withAnthropicBetaFeatures(Collection<String> betaFeatures) {
        Objects.requireNonNull(betaFeatures, "Anthropic beta features must not be null");
        return new ProtocolConversionRequest(
                streaming,
                toolCallingRequired,
                reasoningRequired,
                routeContext,
                betaFeatures.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(feature -> !feature.isEmpty())
                        .distinct()
                        .toList()
        );
    }

    public ProtocolConversionRequest forRoute(long providerChannelId, String upstreamModel) {
        return new ProtocolConversionRequest(
                streaming,
                toolCallingRequired,
                reasoningRequired,
                new ProtocolConversionRouteContext(providerChannelId, upstreamModel),
                anthropicBetaFeatures
        );
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

    public ProtocolConversionRouteContext routeContext() {
        return routeContext;
    }

    public List<String> anthropicBetaFeatures() {
        return anthropicBetaFeatures;
    }
}
