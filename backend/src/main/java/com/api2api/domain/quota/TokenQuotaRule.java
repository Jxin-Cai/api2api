package com.api2api.domain.quota;

import java.math.BigDecimal;
import java.util.Objects;

/** A budget shared by a group, or restricted to one key. Empty model means all models combined. */
public record TokenQuotaRule(long id, QuotaScope scope, long targetId, String model,
                             QuotaPeriod period, BigDecimal limitTokens, boolean enabled) {
    public TokenQuotaRule {
        Objects.requireNonNull(scope, "Quota scope must not be null");
        Objects.requireNonNull(period, "Quota period must not be null");
        Objects.requireNonNull(limitTokens, "Token limit must not be null");
        if (targetId <= 0 || limitTokens.signum() <= 0) {
            throw new IllegalArgumentException("Quota target and limit must be positive");
        }
        model = model == null ? "" : model.trim();
        if (model.length() > 256) throw new IllegalArgumentException("Model name is too long");
    }

    public boolean appliesTo(String requestedModel) {
        return enabled && (model.isEmpty() || model.equals(requestedModel));
    }
}
