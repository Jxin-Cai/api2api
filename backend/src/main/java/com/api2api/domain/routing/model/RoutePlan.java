package com.api2api.domain.routing.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable ordered route plan generated for one routing request.
 */
public final class RoutePlan {

    private final RoutingRequest routingRequest;
    private final List<RouteCandidate> candidates;
    private final Instant createdAt;

    private RoutePlan(RoutingRequest routingRequest, List<RouteCandidate> candidates, Instant createdAt) {
        this.routingRequest = Objects.requireNonNull(routingRequest, "Routing request must not be null");
        Objects.requireNonNull(candidates, "Route candidates must not be null");
        this.candidates = List.copyOf(candidates);
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
    }

    public static RoutePlan of(RoutingRequest routingRequest, List<RouteCandidate> candidates, Instant createdAt) {
        return new RoutePlan(routingRequest, candidates, createdAt);
    }

    public boolean hasCandidate() {
        return !candidates.isEmpty();
    }

    public RouteCandidate firstCandidate() {
        if (!hasCandidate()) {
            throw new IllegalStateException("No route candidate is available for requested model");
        }
        return candidates.get(0);
    }

    public RoutingRequest routingRequest() {
        return routingRequest;
    }

    public List<RouteCandidate> candidates() {
        return candidates;
    }

    /**
     * Keeps only routes that speak the client's protocol. Auxiliary endpoints such as token counting
     * have no counterpart after a protocol conversion, so converted candidates are not usable.
     */
    public RoutePlan nativeProtocolOnly() {
        List<RouteCandidate> nativeCandidates = candidates.stream()
                .filter(candidate -> !candidate.requiresProtocolConversion())
                .toList();
        if (nativeCandidates.size() == candidates.size()) {
            return this;
        }
        return new RoutePlan(routingRequest, nativeCandidates, createdAt);
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoutePlan routePlan)) {
            return false;
        }
        return Objects.equals(routingRequest, routePlan.routingRequest)
                && Objects.equals(candidates, routePlan.candidates)
                && Objects.equals(createdAt, routePlan.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routingRequest, candidates, createdAt);
    }
}
