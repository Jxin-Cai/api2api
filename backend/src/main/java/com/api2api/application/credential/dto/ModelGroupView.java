package com.api2api.application.credential.dto;

import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelName;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Model group enriched with today's per-model consumption and the models currently capped. */
public record ModelGroupView(
        ModelGroup group,
        Map<ModelName, BigDecimal> todayUsageByModel,
        Set<ModelName> rateLimitedModels
) {

    public ModelGroupView {
        Objects.requireNonNull(group, "Model group must not be null");
        todayUsageByModel = Map.copyOf(Objects.requireNonNull(todayUsageByModel, "Today usage must not be null"));
        rateLimitedModels = Set.copyOf(Objects.requireNonNull(rateLimitedModels, "Rate limited models must not be null"));
    }

    public static ModelGroupView of(ModelGroup group, Map<ModelName, BigDecimal> todayUsageByModel) {
        Map<ModelName, BigDecimal> usage = todayUsageByModel == null ? Map.of() : todayUsageByModel;
        return new ModelGroupView(group, usage, group.rateLimitedModels(usage));
    }
}
