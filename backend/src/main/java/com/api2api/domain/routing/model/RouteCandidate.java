package com.api2api.domain.routing.model;

import com.api2api.domain.channel.model.ModelMappingResult;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.protocol.model.ConversionRoute;
import java.util.Objects;

/**
 * Candidate route selected from a provider channel, model support entry and protocol conversion route.
 */
public final class RouteCandidate {

    private final ProviderChannelId providerChannelId;
    private final ProviderChannelName providerChannelName;
    private final ModelName requestedModel;
    private final ModelName upstreamModel;
    private final ProtocolType clientProtocol;
    private final ProtocolType upstreamProtocol;
    private final RoutePriority priority;
    private final int routePriority;
    private final boolean preferred;
    private final ConversionRoute conversionRoute;
    private final ModelMappingResult modelMappingResult;

    private RouteCandidate(
            ProviderChannelId providerChannelId,
            ProviderChannelName providerChannelName,
            ModelName requestedModel,
            ModelName upstreamModel,
            ProtocolType clientProtocol,
            ProtocolType upstreamProtocol,
            RoutePriority priority,
            int routePriority,
            boolean preferred,
            ConversionRoute conversionRoute,
            ModelMappingResult modelMappingResult
    ) {
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.providerChannelName = Objects.requireNonNull(providerChannelName, "Provider channel name must not be null");
        this.requestedModel = Objects.requireNonNull(requestedModel, "Requested model must not be null");
        this.upstreamModel = Objects.requireNonNull(upstreamModel, "Upstream model must not be null");
        this.clientProtocol = Objects.requireNonNull(clientProtocol, "Client protocol must not be null");
        this.upstreamProtocol = Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null");
        this.priority = Objects.requireNonNull(priority, "Route priority must not be null");
        this.routePriority = routePriority;
        this.preferred = preferred;
        this.conversionRoute = Objects.requireNonNull(conversionRoute, "Conversion route must not be null");
        this.modelMappingResult = Objects.requireNonNull(modelMappingResult, "Model mapping result must not be null");
        if (!conversionRoute.targetProtocol().equals(upstreamProtocol)) {
            throw new IllegalArgumentException("Conversion route target protocol must equal upstream protocol");
        }
        if (!conversionRoute.sourceProtocol().equals(clientProtocol)) {
            throw new IllegalArgumentException("Conversion route source protocol must equal client protocol");
        }
        if (!modelMappingResult.requestedModel().equals(requestedModel)
                || !modelMappingResult.upstreamModel().equals(upstreamModel)) {
            throw new IllegalArgumentException("Model mapping result must match requested and upstream model");
        }
    }

    public static RouteCandidate of(
            ProviderChannelId providerChannelId,
            ProviderChannelName providerChannelName,
            ModelName requestedModel,
            ModelName upstreamModel,
            ProtocolType clientProtocol,
            ProtocolType upstreamProtocol,
            RoutePriority priority,
            int routePriority,
            boolean preferred,
            ConversionRoute conversionRoute,
            ModelMappingResult modelMappingResult
    ) {
        return new RouteCandidate(
                providerChannelId,
                providerChannelName,
                requestedModel,
                upstreamModel,
                clientProtocol,
                upstreamProtocol,
                priority,
                routePriority,
                preferred,
                conversionRoute,
                modelMappingResult
        );
    }

    public boolean requiresProtocolConversion() {
        return !clientProtocol.isSameAs(upstreamProtocol);
    }

    public boolean requiresModelMapping() {
        return !upstreamModel.equals(requestedModel);
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public ProviderChannelName providerChannelName() {
        return providerChannelName;
    }

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ModelName upstreamModel() {
        return upstreamModel;
    }

    public ProtocolType clientProtocol() {
        return clientProtocol;
    }

    public ProtocolType upstreamProtocol() {
        return upstreamProtocol;
    }

    public RoutePriority priority() {
        return priority;
    }

    public int routePriority() {
        return routePriority;
    }

    public boolean preferred() {
        return preferred;
    }

    public ConversionRoute conversionRoute() {
        return conversionRoute;
    }

    public ModelMappingResult modelMappingResult() {
        return modelMappingResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteCandidate that)) {
            return false;
        }
        return Objects.equals(providerChannelId, that.providerChannelId)
                && Objects.equals(providerChannelName, that.providerChannelName)
                && Objects.equals(requestedModel, that.requestedModel)
                && Objects.equals(upstreamModel, that.upstreamModel)
                && clientProtocol == that.clientProtocol
                && upstreamProtocol == that.upstreamProtocol
                && Objects.equals(priority, that.priority)
                && routePriority == that.routePriority
                && preferred == that.preferred
                && Objects.equals(conversionRoute, that.conversionRoute)
                && Objects.equals(modelMappingResult, that.modelMappingResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                providerChannelId,
                providerChannelName,
                requestedModel,
                upstreamModel,
                clientProtocol,
                upstreamProtocol,
                priority,
                routePriority,
                preferred,
                conversionRoute,
                modelMappingResult
        );
    }
}
