package com.api2api.domain.routing.model;

import com.api2api.domain.channel.model.ProviderChannelId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Diagnostic information for a failed route attempt.
 */
public final class RouteFailure {

    private static final int MAX_REASON_LENGTH = 1000;

    private final ProviderChannelId providerChannelId;
    private final RouteFailureType failureType;
    private final String reason;
    private final boolean retryable;
    private final Instant occurredAt;
    private final Duration retryAfter;

    private RouteFailure(
            ProviderChannelId providerChannelId,
            RouteFailureType failureType,
            String reason,
            boolean retryable,
            Instant occurredAt,
            Duration retryAfter
    ) {
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.failureType = Objects.requireNonNull(failureType, "Route failure type must not be null");
        this.reason = normalizeReason(reason);
        this.retryable = retryable;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred time must not be null");
        this.retryAfter = normalizeRetryAfter(retryAfter);
    }

    public static RouteFailure of(
            ProviderChannelId providerChannelId,
            RouteFailureType failureType,
            String reason,
            boolean retryable,
            Instant occurredAt
    ) {
        return new RouteFailure(providerChannelId, failureType, reason, retryable, occurredAt, null);
    }

    public static RouteFailure of(
            ProviderChannelId providerChannelId,
            RouteFailureType failureType,
            String reason,
            boolean retryable,
            Instant occurredAt,
            Duration retryAfter
    ) {
        return new RouteFailure(providerChannelId, failureType, reason, retryable, occurredAt, retryAfter);
    }

    public static RouteFailure withDefaultRetryable(
            ProviderChannelId providerChannelId,
            RouteFailureType failureType,
            String reason,
            Instant occurredAt
    ) {
        Objects.requireNonNull(failureType, "Route failure type must not be null");
        return new RouteFailure(
                providerChannelId, failureType, reason, failureType.isRetryableByDefault(), occurredAt, null);
    }

    private static Duration normalizeRetryAfter(Duration retryAfter) {
        if (retryAfter == null || retryAfter.isZero() || retryAfter.isNegative()) {
            return null;
        }
        return retryAfter;
    }

    private static String normalizeReason(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Failure reason must not be null");
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Failure reason length must be between 1 and 1000");
        }
        return trimmed;
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public RouteFailureType failureType() {
        return failureType;
    }

    public String reason() {
        return reason;
    }

    public boolean retryable() {
        return retryable;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    /**
     * Backoff window advertised by the provider, when it supplied one.
     */
    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteFailure that)) {
            return false;
        }
        return retryable == that.retryable
                && Objects.equals(providerChannelId, that.providerChannelId)
                && failureType == that.failureType
                && Objects.equals(reason, that.reason)
                && Objects.equals(occurredAt, that.occurredAt)
                && Objects.equals(retryAfter, that.retryAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerChannelId, failureType, reason, retryable, occurredAt, retryAfter);
    }
}
