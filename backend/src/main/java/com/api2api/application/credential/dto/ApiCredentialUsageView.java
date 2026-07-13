package com.api2api.application.credential.dto;

import com.api2api.domain.credential.model.ApiCredential;
import java.util.Objects;

public final class ApiCredentialUsageView {

    private final ApiCredential credential;
    private final long consumedTokens;
    private final long todayConsumedTokens;

    private ApiCredentialUsageView(ApiCredential credential, long consumedTokens, long todayConsumedTokens) {
        if (consumedTokens < 0) {
            throw new IllegalArgumentException("Consumed tokens must not be negative");
        }
        if (todayConsumedTokens < 0) {
            throw new IllegalArgumentException("Today consumed tokens must not be negative");
        }
        this.credential = Objects.requireNonNull(credential, "API credential must not be null");
        this.consumedTokens = consumedTokens;
        this.todayConsumedTokens = todayConsumedTokens;
    }

    public static ApiCredentialUsageView of(
            ApiCredential credential,
            long consumedTokens,
            long todayConsumedTokens
    ) {
        return new ApiCredentialUsageView(credential, consumedTokens, todayConsumedTokens);
    }

    public ApiCredential credential() {
        return credential;
    }

    public long consumedTokens() {
        return consumedTokens;
    }

    public long todayConsumedTokens() {
        return todayConsumedTokens;
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

    public long getTodayConsumedTokens() {
        return todayConsumedTokens;
    }

    public Long getRemainingTokens() {
        return remainingTokens();
    }
}
