package com.api2api.domain.analytics.model;

import java.util.List;
import java.util.Objects;

/**
 * Per-credential dashboard metrics for the current front-portal user:
 * today/month top credential rankings plus a per-credential token trend.
 */
public final class FrontKeyMetrics {

    private final List<CredentialTokenRanking> dailyTopCredentials;
    private final List<CredentialTokenRanking> monthlyTopCredentials;
    private final List<CredentialTokenTrendPoint> credentialTokenTrends;
    private final List<CredentialConcurrencyTrendPoint> credentialConcurrencyTrends;

    private FrontKeyMetrics(
            List<CredentialTokenRanking> dailyTopCredentials,
            List<CredentialTokenRanking> monthlyTopCredentials,
            List<CredentialTokenTrendPoint> credentialTokenTrends,
            List<CredentialConcurrencyTrendPoint> credentialConcurrencyTrends
    ) {
        this.dailyTopCredentials = requireList(dailyTopCredentials, "Daily top credentials");
        this.monthlyTopCredentials = requireList(monthlyTopCredentials, "Monthly top credentials");
        this.credentialTokenTrends = requireList(credentialTokenTrends, "Credential token trends");
        this.credentialConcurrencyTrends = requireList(credentialConcurrencyTrends, "Credential concurrency trends");
    }

    public static FrontKeyMetrics of(
            List<CredentialTokenRanking> dailyTopCredentials,
            List<CredentialTokenRanking> monthlyTopCredentials,
            List<CredentialTokenTrendPoint> credentialTokenTrends,
            List<CredentialConcurrencyTrendPoint> credentialConcurrencyTrends
    ) {
        return new FrontKeyMetrics(
                dailyTopCredentials, monthlyTopCredentials, credentialTokenTrends, credentialConcurrencyTrends);
    }

    private static <T> List<T> requireList(List<T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        return values.stream()
                .map(value -> Objects.requireNonNull(value, name + " must not contain null elements"))
                .toList();
    }

    public List<CredentialTokenRanking> dailyTopCredentials() {
        return dailyTopCredentials;
    }

    public List<CredentialTokenRanking> monthlyTopCredentials() {
        return monthlyTopCredentials;
    }

    public List<CredentialTokenTrendPoint> credentialTokenTrends() {
        return credentialTokenTrends;
    }

    public List<CredentialConcurrencyTrendPoint> credentialConcurrencyTrends() {
        return credentialConcurrencyTrends;
    }

    public List<CredentialTokenRanking> getDailyTopCredentials() {
        return dailyTopCredentials;
    }

    public List<CredentialTokenRanking> getMonthlyTopCredentials() {
        return monthlyTopCredentials;
    }

    public List<CredentialTokenTrendPoint> getCredentialTokenTrends() {
        return credentialTokenTrends;
    }

    public List<CredentialConcurrencyTrendPoint> getCredentialConcurrencyTrends() {
        return credentialConcurrencyTrends;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontKeyMetrics that)) {
            return false;
        }
        return Objects.equals(dailyTopCredentials, that.dailyTopCredentials)
                && Objects.equals(monthlyTopCredentials, that.monthlyTopCredentials)
                && Objects.equals(credentialTokenTrends, that.credentialTokenTrends)
                && Objects.equals(credentialConcurrencyTrends, that.credentialConcurrencyTrends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dailyTopCredentials, monthlyTopCredentials, credentialTokenTrends, credentialConcurrencyTrends);
    }
}
