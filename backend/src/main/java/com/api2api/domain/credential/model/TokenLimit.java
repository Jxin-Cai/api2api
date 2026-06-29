package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * Cumulative token limit for an API credential. A value of zero means unlimited.
 */
public final class TokenLimit {

    private final long value;

    private TokenLimit(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Token limit must not be negative");
        }
        this.value = value;
    }

    public static TokenLimit of(long value) {
        return new TokenLimit(value);
    }

    public static TokenLimit unlimited() {
        return new TokenLimit(0);
    }

    public boolean isUnlimited() {
        return value == 0;
    }

    public boolean isExceededBy(long consumedTokens) {
        if (consumedTokens < 0) {
            throw new IllegalArgumentException("Consumed tokens must not be negative");
        }
        return !isUnlimited() && consumedTokens >= value;
    }

    public long value() {
        return value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenLimit that)) {
            return false;
        }
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "TokenLimit{" +
                "value=" + value +
                '}';
    }
}
