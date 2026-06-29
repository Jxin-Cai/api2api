package com.api2api.domain.routing.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record of one route candidate attempt.
 */
public final class RouteAttempt {

    private final RouteCandidate candidate;
    private final int attemptOrder;
    private final Instant startedAt;
    private final Instant endedAt;
    private final RouteFailure failure;

    private RouteAttempt(
            RouteCandidate candidate,
            int attemptOrder,
            Instant startedAt,
            Instant endedAt,
            RouteFailure failure
    ) {
        this.candidate = Objects.requireNonNull(candidate, "Route candidate must not be null");
        if (attemptOrder < 1) {
            throw new IllegalArgumentException("Attempt order must be greater than or equal to 1");
        }
        this.attemptOrder = attemptOrder;
        this.startedAt = Objects.requireNonNull(startedAt, "Started time must not be null");
        if (endedAt != null && endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Ended time must not be before started time");
        }
        this.endedAt = endedAt;
        this.failure = failure;
    }

    public static RouteAttempt start(RouteCandidate candidate, int attemptOrder, Instant startedAt) {
        return new RouteAttempt(candidate, attemptOrder, startedAt, null, null);
    }

    public static RouteAttempt of(
            RouteCandidate candidate,
            int attemptOrder,
            Instant startedAt,
            Instant endedAt,
            RouteFailure failure
    ) {
        return new RouteAttempt(candidate, attemptOrder, startedAt, endedAt, failure);
    }

    public RouteAttempt markFailed(RouteFailure failure, Instant endedAt) {
        Objects.requireNonNull(failure, "Route failure must not be null");
        if (!failure.providerChannelId().equals(candidate.providerChannelId())) {
            throw new IllegalArgumentException("Route failure channel must match attempt candidate channel");
        }
        return new RouteAttempt(candidate, attemptOrder, startedAt, endedAt, failure);
    }

    public boolean failed() {
        return failure != null;
    }

    public RouteCandidate candidate() {
        return candidate;
    }

    public int attemptOrder() {
        return attemptOrder;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public RouteFailure failure() {
        return failure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteAttempt that)) {
            return false;
        }
        return attemptOrder == that.attemptOrder
                && Objects.equals(candidate, that.candidate)
                && Objects.equals(startedAt, that.startedAt)
                && Objects.equals(endedAt, that.endedAt)
                && Objects.equals(failure, that.failure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidate, attemptOrder, startedAt, endedAt, failure);
    }
}
