package com.api2api.domain.routing.model;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ConversionRequirement;
import java.util.Objects;

/**
 * Client routing request containing protocol, requested model and conversion requirement.
 */
public final class RoutingRequest {

    private final ProtocolType requestProtocol;
    private final ModelName requestedModel;
    private final ConversionRequirement requirement;

    private RoutingRequest(ProtocolType requestProtocol, ModelName requestedModel, ConversionRequirement requirement) {
        this.requestProtocol = Objects.requireNonNull(requestProtocol, "Request protocol must not be null");
        this.requestedModel = Objects.requireNonNull(requestedModel, "Requested model must not be null");
        this.requirement = Objects.requireNonNull(requirement, "Conversion requirement must not be null");
    }

    public static RoutingRequest of(
            ProtocolType requestProtocol,
            ModelName requestedModel,
            ConversionRequirement requirement
    ) {
        return new RoutingRequest(requestProtocol, requestedModel, requirement);
    }

    public ProtocolType requestProtocol() {
        return requestProtocol;
    }

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ConversionRequirement requirement() {
        return requirement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoutingRequest that)) {
            return false;
        }
        return requestProtocol == that.requestProtocol
                && Objects.equals(requestedModel, that.requestedModel)
                && Objects.equals(requirement, that.requirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestProtocol, requestedModel, requirement);
    }
}
