package com.api2api.domain.analytics.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Non-negative token amount used by analytics read models.
 */
public final class TokenAmount {

    private static final double MILLION = 1_000_000.0d;

    private final BigDecimal tokens;

    private TokenAmount(BigDecimal tokens) {
        BigDecimal nonNullTokens = Objects.requireNonNull(tokens, "Token amount must not be null");
        if (nonNullTokens.signum() < 0) {
            throw new IllegalArgumentException("Token amount must be greater than or equal to 0");
        }
        this.tokens = nonNullTokens.stripTrailingZeros();
    }

    public static TokenAmount of(long tokens) {
        return new TokenAmount(BigDecimal.valueOf(tokens));
    }

    public static TokenAmount of(BigDecimal tokens) {
        return new TokenAmount(tokens);
    }

    public static TokenAmount zero() {
        return new TokenAmount(BigDecimal.ZERO);
    }

    public double toMillions() {
        return tokens.doubleValue() / MILLION;
    }

    public TokenAmount plus(TokenAmount other) {
        TokenAmount nonNullOther = Objects.requireNonNull(other, "Other token amount must not be null");
        return new TokenAmount(tokens.add(nonNullOther.tokens));
    }

    public BigDecimal tokens() {
        return tokens;
    }

    public BigDecimal getTokens() {
        return tokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenAmount that)) {
            return false;
        }
        return tokens.compareTo(that.tokens) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokens);
    }

    @Override
    public String toString() {
        return String.valueOf(tokens);
    }
}
