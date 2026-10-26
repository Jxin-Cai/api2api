package com.api2api.application.quota;

import com.api2api.domain.quota.TokenQuotaRule;
import java.time.Instant;

/** A caller budget failure, never a provider/channel failure. */
public final class TokenQuotaExceededException extends RuntimeException {
    private final Instant resetAt;

    public TokenQuotaExceededException(TokenQuotaRule rule, Instant resetAt) {
        super("TOKEN_RATE_LIMIT_EXCEEDED: " + rule.scope() + " " + rule.period()
                + " token budget exhausted" + (rule.model().isEmpty() ? "" : " for model " + rule.model()));
        this.resetAt = resetAt;
    }

    public Instant resetAt() { return resetAt; }
}
