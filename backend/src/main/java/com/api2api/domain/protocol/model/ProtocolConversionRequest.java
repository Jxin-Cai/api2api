package com.api2api.domain.protocol.model;

/**
 * 本次调用对协议转换能力的需求。
 */
public final class ProtocolConversionRequest {
    private final boolean streaming;
    private final boolean toolCallingRequired;
    private final boolean reasoningRequired;
    private final ProtocolConversionRouteContext routeContext;

    private ProtocolConversionRequest(
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

    public static ProtocolConversionRequest of(boolean streaming, boolean toolCallingRequired, boolean reasoningRequired) {
        return new ProtocolConversionRequest(streaming, toolCallingRequired, reasoningRequired, null);
    }

    public ProtocolConversionRequest forRoute(long providerChannelId, String upstreamModel) {
        return new ProtocolConversionRequest(
                streaming,
                toolCallingRequired,
                reasoningRequired,
                new ProtocolConversionRouteContext(providerChannelId, upstreamModel)
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
}
