package com.api2api.application.credential.dto;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ModelName;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public final class ApiCredentialUsageView {

    private final ApiCredential credential;
    private final BigDecimal consumedTokens;
    private final long totalTokens;
    private final BigDecimal todayConsumedTokens;
    private final long todayTotalTokens;
    /** Models capped today by the bound model group's daily limit; shared by every key of the group. */
    private final Set<ModelName> rateLimitedModels;

    private ApiCredentialUsageView(ApiCredential credential, BigDecimal consumedTokens, long totalTokens,
                                   BigDecimal todayConsumedTokens, long todayTotalTokens,
                                   Set<ModelName> rateLimitedModels) {
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
        this.rateLimitedModels = Set.copyOf(Objects.requireNonNull(rateLimitedModels, "Rate limited models must not be null"));
    }

    public static ApiCredentialUsageView of(
            ApiCredential credential,
            BigDecimal consumedTokens,
            long totalTokens,
            BigDecimal todayConsumedTokens,
            long todayTotalTokens
    ) {
        return of(credential, consumedTokens, totalTokens, todayConsumedTokens, todayTotalTokens, Set.of());
    }

    public static ApiCredentialUsageView of(
            ApiCredential credential,
            BigDecimal consumedTokens,
            long totalTokens,
            BigDecimal todayConsumedTokens,
            long todayTotalTokens,
            Set<ModelName> rateLimitedModels
    ) {
        return new ApiCredentialUsageView(
                credential, consumedTokens, totalTokens, todayConsumedTokens, todayTotalTokens, rateLimitedModels);
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

    public Set<ModelName> rateLimitedModels() {
        return rateLimitedModels;
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

    public Set<ModelName> getRateLimitedModels() {
        return rateLimitedModels;
    }

    public BigDecimal getRemainingTokens() {
        return remainingTokens();
    }
}
