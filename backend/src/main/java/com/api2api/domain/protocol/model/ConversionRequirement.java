package com.api2api.domain.protocol.model;

/**
 * 设计文档中的转换需求值对象，语义等同于 ProtocolConversionRequest。
 */
public final class ConversionRequirement {
    private final boolean streaming;
    private final boolean toolCallingRequired;
    private final boolean reasoningRequired;
    private final ProtocolConversionRouteContext routeContext;

    private ConversionRequirement(
            boolean streaming,
            boolean toolCallingRequired,
            boolean reasoningRequired,
            ProtocolConversionRouteContext routeContext
    ) {
        this.streaming = streaming;
        this.toolCallingRequired = toolCallingRequired;
        this.reasoningRequired = reasoningRequired;
        this.routeContext = routeContext;
    }

    public static ConversionRequirement of(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        return new ConversionRequirement(streaming, toolCallingRequired, reasoningRequired, null);
    }

    public ConversionRequirement forRoute(long providerChannelId, String upstreamModel) {
        return new ConversionRequirement(
                streaming,
                toolCallingRequired,
                reasoningRequired,
                new ProtocolConversionRouteContext(providerChannelId, upstreamModel)
        );
    }

    public ProtocolConversionRequest toProtocolConversionRequest() {
        ProtocolConversionRequest request = ProtocolConversionRequest.of(
                streaming, toolCallingRequired, reasoningRequired);
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
