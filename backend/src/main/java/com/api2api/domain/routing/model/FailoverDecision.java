package com.api2api.domain.routing.model;

import java.util.List;
import java.util.Objects;

/**
 * Failover policy decision after a route failure.
 */
public final class FailoverDecision {

    private final FailoverAction action;
    private final RouteCandidate nextCandidate;
    private final List<RouteFailure> failures;
    private final String reason;

    private FailoverDecision(
            FailoverAction action,
            RouteCandidate nextCandidate,
            List<RouteFailure> failures,
            String reason
    ) {
        this.action = Objects.requireNonNull(action, "Failover action must not be null");
        if (action == FailoverAction.RETRY_NEXT && nextCandidate == null) {
            throw new IllegalArgumentException("Next candidate is required when retrying next route");
        }
        if (action == FailoverAction.STOP && nextCandidate != null) {
            throw new IllegalArgumentException("Next candidate must be empty when failover stops");
        }
        Objects.requireNonNull(failures, "Route failures must not be null");
        this.nextCandidate = nextCandidate;
        this.failures = List.copyOf(failures);
        this.reason = normalizeReason(reason);
    }

    public static FailoverDecision retryNext(RouteCandidate nextCandidate, List<RouteFailure> failures, String reason) {
        return new FailoverDecision(FailoverAction.RETRY_NEXT, nextCandidate, failures, reason);
    }

    public static FailoverDecision stop(List<RouteFailure> failures, String reason) {
        return new FailoverDecision(FailoverAction.STOP, null, failures, reason);
    }

    private static String normalizeReason(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Failover decision reason must not be null");
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Failover decision reason must not be empty");
        }
        return trimmed;
    }

    public FailoverAction action() {
        return action;
    }

    public RouteCandidate nextCandidate() {
        return nextCandidate;
    }

    public List<RouteFailure> failures() {
        return failures;
    }

    public String reason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FailoverDecision that)) {
            return false;
        }
        return action == that.action
                && Objects.equals(nextCandidate, that.nextCandidate)
                && Objects.equals(failures, that.failures)
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, nextCandidate, failures, reason);
    }
}
