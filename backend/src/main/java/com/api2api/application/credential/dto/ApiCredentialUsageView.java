package com.api2api.application.credential.dto;

import com.api2api.domain.credential.model.ApiCredential;
import java.math.BigDecimal;
import java.util.Objects;

public final class ApiCredentialUsageView {

    private final ApiCredential credential;
    private final BigDecimal consumedTokens;
    private final long totalTokens;
    private final BigDecimal todayConsumedTokens;
    private final long todayTotalTokens;

    private ApiCredentialUsageView(ApiCredential credential, BigDecimal consumedTokens, long totalTokens, BigDecimal todayConsumedTokens, long todayTotalTokens) {
        BigDecimal nonNullConsumedTokens = Objects.requireNonNull(consumedTokens, "Consumed tokens must not be null");
        BigDecimal nonNullTodayConsumedTokens = Objects.requireNonNull(todayConsumedTokens, "Today consumed tokens must not be null");
        if (nonNullConsumedTokens.signum() < 0) {
            throw new IllegalArgumentException("Consumed tokens must not be negative");
        }
        if (totalTokens < 0 || todayTotalTokens < 0 || nonNullTodayConsumedTokens.signum() < 0) {
            throw new IllegalArgumentException("Today consumed tokens must not be negative");
        }
        this.credential = Objects.requireNonNull(credential, "API credential must not be null");
        this.consumedTokens = nonNullConsumedTokens;
        this.totalTokens = totalTokens;
        this.todayConsumedTokens = nonNullTodayConsumedTokens;
        this.todayTotalTokens = todayTotalTokens;
    }

    public static ApiCredentialUsageView of(
            ApiCredential credential,
            BigDecimal consumedTokens,
            long totalTokens,
            BigDecimal todayConsumedTokens,
            long todayTotalTokens
    ) {
        return new ApiCredentialUsageView(credential, consumedTokens, totalTokens, todayConsumedTokens, todayTotalTokens);
    }

    public ApiCredential credential() {
        return credential;
    }

    public BigDecimal consumedTokens() {
        return consumedTokens;
    }

    public long totalTokens() {
        return totalTokens;
    }

    public BigDecimal todayConsumedTokens() {
        return todayConsumedTokens;
    }

    public long todayTotalTokens() {
        return todayTotalTokens;
    }

    public BigDecimal remainingTokens() {
        if (credential.getTokenLimit().isUnlimited()) {
            return null;
        }
        return BigDecimal.valueOf(credential.getTokenLimit().getValue()).subtract(consumedTokens).max(BigDecimal.ZERO);
    }

    public ApiCredential getCredential() {
        return credential;
    }

    public BigDecimal getConsumedTokens() {
        return consumedTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public BigDecimal getTodayConsumedTokens() {
        return todayConsumedTokens;
    }

    public long getTodayTotalTokens() {
        return todayTotalTokens;
    }

    public BigDecimal getRemainingTokens() {
        return remainingTokens();
    }
}
