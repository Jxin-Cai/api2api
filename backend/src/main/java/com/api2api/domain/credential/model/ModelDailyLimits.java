package com.api2api.domain.credential.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable per-model daily token caps of a model group. A model without an entry is uncapped.
 * Limits are expressed in weighted "actual" tokens, the same unit as the credential token quota.
 */
public final class ModelDailyLimits {

    private final Map<ModelName, Long> limits;

    private ModelDailyLimits(Map<ModelName, Long> limits) {
        this.limits = normalize(limits);
    }

    public static ModelDailyLimits of(Map<ModelName, Long> limits) {
        return new ModelDailyLimits(limits);
    }

    public static ModelDailyLimits empty() {
        return new ModelDailyLimits(Map.of());
    }

    public Optional<Long> limitFor(ModelName modelName) {
        Objects.requireNonNull(modelName, "Model name must not be null");
        return Optional.ofNullable(limits.get(modelName));
    }

    public boolean isExceeded(ModelName modelName, BigDecimal consumedTodayTokens) {
        BigDecimal consumed = Objects.requireNonNull(consumedTodayTokens, "Consumed tokens must not be null");
        if (consumed.signum() < 0) {
            throw new IllegalArgumentException("Consumed tokens must not be negative");
        }
        return limitFor(modelName)
                .map(limit -> consumed.compareTo(BigDecimal.valueOf(limit)) >= 0)
                .orElse(false);
    }

    public boolean isEmpty() {
        return limits.isEmpty();
    }

    private static Map<ModelName, Long> normalize(Map<ModelName, Long> limits) {
        Objects.requireNonNull(limits, "Model daily limits must not be null");
        Map<ModelName, Long> normalized = new LinkedHashMap<>();
        limits.forEach((model, limit) -> {
            Objects.requireNonNull(model, "Daily limited model must not be null");
            Objects.requireNonNull(limit, "Daily limit must not be null");
            if (limit <= 0) {
                throw new IllegalArgumentException("Daily limit must be greater than 0");
            }
            normalized.put(model, limit);
        });
        return Collections.unmodifiableMap(normalized);
    }

    public Map<ModelName, Long> limits() {
        return limits;
    }

    public Map<ModelName, Long> getLimits() {
        return limits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModelDailyLimits that)) {
            return false;
        }
        return Objects.equals(limits, that.limits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limits);
    }

    @Override
    public String toString() {
        return "ModelDailyLimits{limits=" + limits + '}';
    }
}
