package com.api2api.domain.gateway.model;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.routing.model.RouteCandidate;
import java.util.Objects;

/**
 * Final result emitted by one gateway invocation for usage recording and diagnostics.
 */
public final class GatewayInvocationResult {

    private final InvocationStatus status;
    private final RouteCandidate finalCandidate;
    private final ProtocolType upstreamProtocol;
    private final ModelName upstreamModel;
    private final UnifiedTokenUsage usage;
    private final boolean streaming;
    private final InvocationError error;

    private GatewayInvocationResult(
            InvocationStatus status,
            RouteCandidate finalCandidate,
            ProtocolType upstreamProtocol,
            ModelName upstreamModel,
            UnifiedTokenUsage usage,
            boolean streaming,
            InvocationError error
    ) {
        this.status = Objects.requireNonNull(status, "Invocation status must not be null");
        validateByStatus(status, finalCandidate, upstreamProtocol, upstreamModel, error);
        this.finalCandidate = finalCandidate;
        this.upstreamProtocol = upstreamProtocol;
        this.upstreamModel = upstreamModel;
        this.usage = usage;
        this.streaming = streaming;
        this.error = error;
    }

    public static GatewayInvocationResult success(RouteCandidate finalCandidate, UnifiedTokenUsage usage, boolean streaming) {
        Objects.requireNonNull(finalCandidate, "Final route candidate must not be null");
        return new GatewayInvocationResult(
                InvocationStatus.SUCCESS,
                finalCandidate,
                finalCandidate.upstreamProtocol(),
                finalCandidate.upstreamModel(),
                usage,
                streaming,
                null
        );
    }

    public static GatewayInvocationResult failed(InvocationError error, boolean streaming) {
        return new GatewayInvocationResult(InvocationStatus.FAILED, null, null, null, null, streaming, error);
    }

    private static void validateByStatus(
            InvocationStatus status,
            RouteCandidate finalCandidate,
            ProtocolType upstreamProtocol,
            ModelName upstreamModel,
            InvocationError error
    ) {
        if (status == InvocationStatus.SUCCESS) {
            Objects.requireNonNull(finalCandidate, "Final route candidate must not be null for successful invocation");
            Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null for successful invocation");
            Objects.requireNonNull(upstreamModel, "Upstream model must not be null for successful invocation");
            if (error != null) {
                throw new IllegalArgumentException("Successful invocation result must not contain an error");
            }
            return;
        }
        if (status == InvocationStatus.FAILED) {
            Objects.requireNonNull(error, "Invocation error must not be null for failed invocation");
            if (finalCandidate != null || upstreamProtocol != null || upstreamModel != null) {
                throw new IllegalArgumentException("Failed invocation result must not contain final upstream details");
            }
        }
    }

    public InvocationStatus status() {
        return status;
    }

    public RouteCandidate finalCandidate() {
        return finalCandidate;
    }

    public ProtocolType upstreamProtocol() {
        return upstreamProtocol;
    }

    public ModelName upstreamModel() {
        return upstreamModel;
    }

    public UnifiedTokenUsage usage() {
        return usage;
    }

    public boolean streaming() {
        return streaming;
    }

    public InvocationError error() {
        return error;
    }

    public InvocationStatus getStatus() {
        return status;
    }

    public RouteCandidate getFinalCandidate() {
        return finalCandidate;
    }

    public ProtocolType getUpstreamProtocol() {
        return upstreamProtocol;
    }

    public ModelName getUpstreamModel() {
        return upstreamModel;
    }

    public UnifiedTokenUsage getUsage() {
        return usage;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public InvocationError getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GatewayInvocationResult that)) {
            return false;
        }
        return streaming == that.streaming
                && status == that.status
                && Objects.equals(finalCandidate, that.finalCandidate)
                && upstreamProtocol == that.upstreamProtocol
                && Objects.equals(upstreamModel, that.upstreamModel)
                && Objects.equals(usage, that.usage)
                && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, finalCandidate, upstreamProtocol, upstreamModel, usage, streaming, error);
    }
}
