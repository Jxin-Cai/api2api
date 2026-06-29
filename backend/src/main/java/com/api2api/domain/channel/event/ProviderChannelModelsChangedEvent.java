package com.api2api.domain.channel.event;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Domain event emitted when a provider channel model support set changes.
 */
public final class ProviderChannelModelsChangedEvent {

    private final ProviderChannelId providerChannelId;
    private final Set<ModelName> changedModelNames;
    private final Instant occurredAt;

    private ProviderChannelModelsChangedEvent(
            ProviderChannelId providerChannelId,
            Set<ModelName> changedModelNames,
            Instant occurredAt
    ) {
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.changedModelNames = normalizeChangedModelNames(changedModelNames);
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred time must not be null");
    }

    public static ProviderChannelModelsChangedEvent of(
            ProviderChannelId providerChannelId,
            Set<ModelName> changedModelNames,
            Instant occurredAt
    ) {
        return new ProviderChannelModelsChangedEvent(providerChannelId, changedModelNames, occurredAt);
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public Set<ModelName> changedModelNames() {
        return Set.copyOf(changedModelNames);
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    private static Set<ModelName> normalizeChangedModelNames(Set<ModelName> changedModelNames) {
        Objects.requireNonNull(changedModelNames, "Changed model names must not be null");
        Set<ModelName> normalizedModelNames = new LinkedHashSet<>();
        for (ModelName modelName : changedModelNames) {
            normalizedModelNames.add(Objects.requireNonNull(modelName, "Changed model name must not be null"));
        }
        return normalizedModelNames;
    }
}
