package com.api2api.domain.analytics.model;

import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.Username;
import java.util.Comparator;
import java.util.Objects;

/**
 * Stable token-consumption ranking row for a user.
 */
public final class UserTokenRanking {

    public static final Comparator<UserTokenRanking> STABLE_TOKEN_DESC_USER_ASC =
            Comparator.comparing((UserTokenRanking ranking) -> ranking.totalTokens.tokens()).reversed()
                    .thenComparing(ranking -> ranking.userAccountId.getValue());

    private final int rank;
    private final UserAccountId userAccountId;
    private final Username username;
    private final TokenAmount totalTokens;

    private UserTokenRanking(int rank, UserAccountId userAccountId, Username username, TokenAmount totalTokens) {
        if (rank < 1) {
            throw new IllegalArgumentException("User token ranking rank must be greater than or equal to 1");
        }
        this.rank = rank;
        this.userAccountId = Objects.requireNonNull(userAccountId, "User token ranking user account id must not be null");
        this.username = Objects.requireNonNull(username, "User token ranking username must not be null");
        this.totalTokens = Objects.requireNonNull(totalTokens, "User token ranking total tokens must not be null");
    }

    public static UserTokenRanking of(int rank, UserAccountId userAccountId, Username username, TokenAmount totalTokens) {
        return new UserTokenRanking(rank, userAccountId, username, totalTokens);
    }

    public int rank() {
        return rank;
    }

    public UserAccountId userAccountId() {
        return userAccountId;
    }

    public Username username() {
        return username;
    }

    public TokenAmount totalTokens() {
        return totalTokens;
    }

    public int getRank() {
        return rank;
    }

    public UserAccountId getUserAccountId() {
        return userAccountId;
    }

    public Username getUsername() {
        return username;
    }

    public TokenAmount getTotalTokens() {
        return totalTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserTokenRanking that)) {
            return false;
        }
        return rank == that.rank
                && Objects.equals(userAccountId, that.userAccountId)
                && Objects.equals(username, that.username)
                && Objects.equals(totalTokens, that.totalTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, userAccountId, username, totalTokens);
    }
}
