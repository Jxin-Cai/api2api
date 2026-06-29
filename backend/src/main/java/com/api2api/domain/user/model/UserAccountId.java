package com.api2api.domain.user.model;

import java.util.Objects;

/**
 * Unique identifier of a user account.
 */
public final class UserAccountId {

    private final Long value;

    private UserAccountId(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("User account id must not be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("User account id must be greater than 0");
        }
        this.value = value;
    }

    public static UserAccountId of(Long value) {
        return new UserAccountId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserAccountId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "UserAccountId{" +
                "value=" + value +
                '}';
    }
}
