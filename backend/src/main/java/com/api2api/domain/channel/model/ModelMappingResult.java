package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Result of requested model to upstream model mapping.
 */
public final class ModelMappingResult {

    private final ModelName requestedModel;
    private final ModelName upstreamModel;
    private final String mappingChain;

    private ModelMappingResult(ModelName requestedModel, ModelName upstreamModel) {
        this.requestedModel = Objects.requireNonNull(requestedModel, "Requested model must not be null");
        this.upstreamModel = Objects.requireNonNull(upstreamModel, "Upstream model must not be null");
        this.mappingChain = requestedModel.equals(upstreamModel)
                ? ""
                : requestedModel.value() + "→" + upstreamModel.value();
    }

    public static ModelMappingResult of(ModelName requestedModel, ModelName upstreamModel) {
        return new ModelMappingResult(requestedModel, upstreamModel);
    }

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ModelName upstreamModel() {
        return upstreamModel;
    }

    public String mappingChain() {
        return mappingChain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModelMappingResult that)) {
            return false;
        }
        return Objects.equals(requestedModel, that.requestedModel)
                && Objects.equals(upstreamModel, that.upstreamModel)
                && Objects.equals(mappingChain, that.mappingChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestedModel, upstreamModel, mappingChain);
    }
}
