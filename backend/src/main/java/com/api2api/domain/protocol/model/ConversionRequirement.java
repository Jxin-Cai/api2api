package com.api2api.domain.protocol.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 设计文档中的转换需求值对象，语义等同于 ProtocolConversionRequest。
 */
public final class ConversionRequirement {
    private final boolean streaming;
    private final boolean toolCallingRequired;
    private final boolean reasoningRequired;
    private final ProtocolConversionRouteContext routeContext;
    private final List<String> anthropicBetaFeatures;

    private ConversionRequirement(
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

    public static ConversionRequirement of(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        return new ConversionRequirement(streaming, toolCallingRequired, reasoningRequired, null, List.of());
    }

    public ConversionRequirement withAnthropicBetaFeatures(Collection<String> betaFeatures) {
        Objects.requireNonNull(betaFeatures, "Anthropic beta features must not be null");
        return new ConversionRequirement(
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

    public ConversionRequirement forRoute(long providerChannelId, String upstreamModel) {
        return new ConversionRequirement(
                streaming,
                toolCallingRequired,
                reasoningRequired,
                new ProtocolConversionRouteContext(providerChannelId, upstreamModel),
                anthropicBetaFeatures
        );
    }

    public ProtocolConversionRequest toProtocolConversionRequest() {
        ProtocolConversionRequest request = ProtocolConversionRequest.of(
                streaming, toolCallingRequired, reasoningRequired)
                .withAnthropicBetaFeatures(anthropicBetaFeatures);
        return routeContext == null
                ? request
                : request.forRoute(routeContext.providerChannelId(), routeContext.upstreamModel());
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
