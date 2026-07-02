package com.api2api.application.channel.dto;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;
import java.util.Set;

public final class ProviderModelOption {

    private final ModelName model;
    private final int providerCount;
    private final Set<ProtocolType> protocols;

    private ProviderModelOption(ModelName model, int providerCount, Set<ProtocolType> protocols) {
        if (providerCount <= 0) {
            throw new IllegalArgumentException("Provider count must be positive");
        }
        this.model = Objects.requireNonNull(model, "Model name must not be null");
        this.providerCount = providerCount;
        this.protocols = Set.copyOf(Objects.requireNonNull(protocols, "Protocols must not be null"));
    }

    public static ProviderModelOption of(ModelName model, int providerCount, Set<ProtocolType> protocols) {
        return new ProviderModelOption(model, providerCount, protocols);
    }

    public ModelName model() {
        return model;
    }

    public int providerCount() {
        return providerCount;
    }

    public Set<ProtocolType> protocols() {
        return protocols;
    }

    public ModelName getModel() {
        return model;
    }

    public int getProviderCount() {
        return providerCount;
    }

    public Set<ProtocolType> getProtocols() {
        return protocols;
    }
}
