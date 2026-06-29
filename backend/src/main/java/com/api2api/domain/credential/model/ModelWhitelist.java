package com.api2api.domain.credential.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable whitelist of model names allowed for an API credential.
 */
public final class ModelWhitelist {

    private final Set<ModelName> models;

    private ModelWhitelist(Set<ModelName> models) {
        this.models = normalize(models);
    }

    public static ModelWhitelist of(Set<ModelName> models) {
        return new ModelWhitelist(models);
    }

    public static ModelWhitelist empty() {
        return new ModelWhitelist(Set.of());
    }

    public boolean contains(ModelName modelName) {
        Objects.requireNonNull(modelName, "Model name must not be null");
        return models.contains(modelName);
    }

    public ModelWhitelist add(ModelName modelName) {
        Objects.requireNonNull(modelName, "Model name must not be null");
        Set<ModelName> changedModels = new LinkedHashSet<>(models);
        changedModels.add(modelName);
        return new ModelWhitelist(changedModels);
    }

    public ModelWhitelist remove(ModelName modelName) {
        Objects.requireNonNull(modelName, "Model name must not be null");
        Set<ModelName> changedModels = new LinkedHashSet<>(models);
        changedModels.remove(modelName);
        return new ModelWhitelist(changedModels);
    }

    private static Set<ModelName> normalize(Set<ModelName> models) {
        Objects.requireNonNull(models, "Model whitelist must not be null");
        Set<ModelName> normalizedModels = new LinkedHashSet<>();
        for (ModelName model : models) {
            normalizedModels.add(Objects.requireNonNull(model, "Whitelisted model must not be null"));
        }
        return Set.copyOf(normalizedModels);
    }

    public Set<ModelName> models() {
        return models;
    }

    public Set<ModelName> getModels() {
        return models;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModelWhitelist that)) {
            return false;
        }
        return Objects.equals(models, that.models);
    }

    @Override
    public int hashCode() {
        return Objects.hash(models);
    }

    @Override
    public String toString() {
        return "ModelWhitelist{" +
                "models=" + models +
                '}';
    }
}
