package com.api2api.domain.usage.model;

import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelName;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Weighted token consumption of one model aggregated across every credential of a model group.
 */
public record ModelGroupModelUsage(ModelGroupId modelGroupId, ModelName model, BigDecimal actualTokens) {

    public ModelGroupModelUsage {
        Objects.requireNonNull(modelGroupId, "Model group id must not be null");
        Objects.requireNonNull(model, "Model name must not be null");
        Objects.requireNonNull(actualTokens, "Actual tokens must not be null");
        if (actualTokens.signum() < 0) {
            throw new IllegalArgumentException("Actual tokens must not be negative");
        }
    }
}
