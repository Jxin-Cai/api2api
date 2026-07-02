package com.api2api.application.credential.dto;

import com.api2api.domain.credential.model.ApiCredential;
import java.util.Objects;

public final class ApiCredentialUsageView {

    private final ApiCredential credential;
    private final long consumedTokens;

    private ApiCredentialUsageView(ApiCredential credential, long consumedTokens) {
        if (consumedTokens < 0) {
            throw new IllegalArgumentException("Consumed tokens must not be negative");
        }
        this.credential = Objects.requireNonNull(credential, "API credential must not be null");
        this.consumedTokens = consumedTokens;
    }

    public static ApiCredentialUsageView of(ApiCredential credential, long consumedTokens) {
        return new ApiCredentialUsageView(credential, consumedTokens);
    }

    public ApiCredential credential() {
        return credential;
    }

    public long consumedTokens() {
        return consumedTokens;
    }

    public Long remainingTokens() {
        if (credential.getTokenLimit().isUnlimited()) {
            return null;
        }
        return Math.max(0, credential.getTokenLimit().getValue() - consumedTokens);
    }

    public ApiCredential getCredential() {
        return credential;
    }

    public long getConsumedTokens() {
        return consumedTokens;
    }

    public Long getRemainingTokens() {
        return remainingTokens();
    }
}
