package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * Decision describing quota state before and after recording one token consumption.
 */
public final class TokenConsumptionDecision {

    public enum Result {
        ACCEPTED,
        EXHAUSTED_BEFORE_REQUEST,
        EXHAUSTED_AFTER_CONSUMPTION
    }

    private final Result result;
    private final long consumedBefore;
    private final long consumedAfter;
    private final TokenLimit tokenLimit;

    private TokenConsumptionDecision(
            Result result,
            long consumedBefore,
            long consumedAfter,
            TokenLimit tokenLimit
    ) {
        if (consumedBefore < 0) {
            throw new IllegalArgumentException("Consumed tokens before request must not be negative");
        }
        if (consumedAfter < 0) {
            throw new IllegalArgumentException("Consumed tokens after request must not be negative");
        }
        if (consumedAfter < consumedBefore) {
            throw new IllegalArgumentException("Consumed tokens after request must not be less than before request");
        }
        this.result = Objects.requireNonNull(result, "Token consumption decision result must not be null");
        this.consumedBefore = consumedBefore;
        this.consumedAfter = consumedAfter;
        this.tokenLimit = Objects.requireNonNull(tokenLimit, "Token limit must not be null");
    }

    public static TokenConsumptionDecision of(
            Result result,
            long consumedBefore,
            long consumedAfter,
            TokenLimit tokenLimit
    ) {
        return new TokenConsumptionDecision(result, consumedBefore, consumedAfter, tokenLimit);
    }

    public boolean accepted() {
        return result == Result.ACCEPTED;
    }

    public Result result() {
        return result;
    }

    public Result getResult() {
        return result;
    }

    public long consumedBefore() {
        return consumedBefore;
    }

    public long getConsumedBefore() {
        return consumedBefore;
    }

    public long consumedAfter() {
        return consumedAfter;
    }

    public long getConsumedAfter() {
        return consumedAfter;
    }

    public TokenLimit tokenLimit() {
        return tokenLimit;
    }

    public TokenLimit getTokenLimit() {
        return tokenLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenConsumptionDecision that)) {
            return false;
        }
        return consumedBefore == that.consumedBefore
                && consumedAfter == that.consumedAfter
                && result == that.result
                && Objects.equals(tokenLimit, that.tokenLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, consumedBefore, consumedAfter, tokenLimit);
    }

    @Override
    public String toString() {
        return "TokenConsumptionDecision{" +
                "result=" + result +
                ", consumedBefore=" + consumedBefore +
                ", consumedAfter=" + consumedAfter +
                ", tokenLimit=" + tokenLimit +
                '}';
    }
}
