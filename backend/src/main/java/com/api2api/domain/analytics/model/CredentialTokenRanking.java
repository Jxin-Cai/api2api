package com.api2api.domain.analytics.model;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import java.util.Comparator;
import java.util.Objects;

/**
 * Stable token-consumption ranking row for one API credential owned by the current user.
 */
public final class CredentialTokenRanking {

    public static final Comparator<CredentialTokenRanking> STABLE_TOKEN_DESC_CREDENTIAL_ASC =
            Comparator.comparing((CredentialTokenRanking ranking) -> ranking.totalTokens.tokens()).reversed()
                    .thenComparing(ranking -> ranking.credentialId.value());

    private final int rank;
    private final ApiCredentialId credentialId;
    private final ApiCredentialName credentialName;
    private final TokenAmount totalTokens;

    private CredentialTokenRanking(
            int rank,
            ApiCredentialId credentialId,
            ApiCredentialName credentialName,
            TokenAmount totalTokens
    ) {
        if (rank < 1) {
            throw new IllegalArgumentException("Credential token ranking rank must be greater than or equal to 1");
        }
        this.rank = rank;
        this.credentialId = Objects.requireNonNull(credentialId, "Credential token ranking credential id must not be null");
        this.credentialName = Objects.requireNonNull(credentialName, "Credential token ranking credential name must not be null");
        this.totalTokens = Objects.requireNonNull(totalTokens, "Credential token ranking total tokens must not be null");
    }

    public static CredentialTokenRanking of(
            int rank,
            ApiCredentialId credentialId,
            ApiCredentialName credentialName,
            TokenAmount totalTokens
    ) {
        return new CredentialTokenRanking(rank, credentialId, credentialName, totalTokens);
    }

    public int rank() {
        return rank;
    }

    public ApiCredentialId credentialId() {
        return credentialId;
    }

    public ApiCredentialName credentialName() {
        return credentialName;
    }

    public TokenAmount totalTokens() {
        return totalTokens;
    }

    public int getRank() {
        return rank;
    }

    public ApiCredentialId getCredentialId() {
        return credentialId;
    }

    public ApiCredentialName getCredentialName() {
        return credentialName;
    }

    public TokenAmount getTotalTokens() {
        return totalTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CredentialTokenRanking that)) {
            return false;
        }
        return rank == that.rank
                && Objects.equals(credentialId, that.credentialId)
                && Objects.equals(credentialName, that.credentialName)
                && Objects.equals(totalTokens, that.totalTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, credentialId, credentialName, totalTokens);
    }
}
