package com.api2api.domain.analytics.model;

import java.util.Objects;

/**
 * Non-negative token amount used by analytics read models.
 */
public final class TokenAmount {

    private static final double MILLION = 1_000_000.0d;

    private final long tokens;

    private TokenAmount(long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("Token amount must be greater than or equal to 0");
        }
        this.tokens = tokens;
    }

    public static TokenAmount of(long tokens) {
        return new TokenAmount(tokens);
    }

    public static TokenAmount zero() {
        return new TokenAmount(0);
    }

    public double toMillions() {
        return tokens / MILLION;
    }

    public TokenAmount plus(TokenAmount other) {
        TokenAmount nonNullOther = Objects.requireNonNull(other, "Other token amount must not be null");
        return new TokenAmount(Math.addExact(tokens, nonNullOther.tokens));
    }

    public long tokens() {
        return tokens;
    }

    public long getTokens() {
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
        return tokens == that.tokens;
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
