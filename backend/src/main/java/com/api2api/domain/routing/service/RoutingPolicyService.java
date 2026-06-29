package com.api2api.domain.routing.service;

import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ModelMappingResult;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.protocol.model.ConversionRoute;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.routing.model.FailoverDecision;
import com.api2api.domain.routing.model.RouteAttempt;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.model.RouteFailure;
import com.api2api.domain.routing.model.RoutePlan;
import com.api2api.domain.routing.model.RoutingRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Routing policy domain service contract.
 */
public interface RoutingPolicyService {

    default RoutePlan buildRoutePlan(
            RoutingRequest request,
            List<ProviderChannel> channels,
            List<ProtocolConversionDefinition> conversionDefinitions,
            Instant now
    ) {
        Objects.requireNonNull(request, "Routing request must not be null");
        Objects.requireNonNull(channels, "Provider channels must not be null");
        Objects.requireNonNull(conversionDefinitions, "Protocol conversion definitions must not be null");
        Objects.requireNonNull(now, "Current time must not be null");

        List<RouteCandidate> candidates = new ArrayList<>();
        for (ProviderChannel channel : channels) {
            if (channel == null || !channel.isEnabledForRouting()) {
                continue;
            }
            appendCandidatesForChannel(request, conversionDefinitions, candidates, channel);
        }

        List<RouteCandidate> sortedCandidates = candidates.stream()
                .sorted(candidateOrdering())
                .toList();
        return RoutePlan.of(request, sortedCandidates, now);
    }

    default FailoverDecision decideNext(
            RoutePlan routePlan,
            List<RouteAttempt> attempts,
            RouteFailure latestFailure
    ) {
        Objects.requireNonNull(routePlan, "Route plan must not be null");
        Objects.requireNonNull(attempts, "Route attempts must not be null");
        Objects.requireNonNull(latestFailure, "Latest route failure must not be null");

        List<RouteFailure> failures = collectFailures(attempts, latestFailure);
        if (!latestFailure.retryable()) {
            return FailoverDecision.stop(failures, "Latest failure is not retryable");
        }

        Set<ProviderChannelId> attemptedChannelIds = collectAttemptedChannelIds(attempts, latestFailure);
        Optional<RouteCandidate> nextCandidate = routePlan.candidates().stream()
                .filter(candidate -> !attemptedChannelIds.contains(candidate.providerChannelId()))
                .findFirst();

        return nextCandidate
                .map(candidate -> FailoverDecision.retryNext(candidate, failures, "Retrying next available route candidate"))
                .orElseGet(() -> FailoverDecision.stop(failures, "No remaining route candidate is available"));
    }

    private static void appendCandidatesForChannel(
            RoutingRequest request,
            List<ProtocolConversionDefinition> conversionDefinitions,
            List<RouteCandidate> candidates,
            ProviderChannel channel
    ) {
        for (ChannelModelSupport modelSupport : channel.findModelSupports(request.requestedModel())) {
            ProtocolType upstreamProtocol = modelSupport.upstreamProtocol();
            if (!channel.supportsProtocol(upstreamProtocol)) {
                continue;
            }
            resolveRoute(request.requestProtocol(), upstreamProtocol, request.requirement(), conversionDefinitions)
                    .map(route -> RouteCandidate.of(
                            channel.id(),
                            channel.name(),
                            modelSupport.requestedModel(),
                            modelSupport.upstreamModel(),
                            request.requestProtocol(),
                            upstreamProtocol,
                            modelSupport.priority(),
                            route,
                            ModelMappingResult.of(modelSupport.requestedModel(), modelSupport.upstreamModel())
                    ))
                    .ifPresent(candidates::add);
        }
    }

    private static Optional<ConversionRoute> resolveRoute(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ConversionRequirement requirement,
            List<ProtocolConversionDefinition> conversionDefinitions
    ) {
        Objects.requireNonNull(sourceProtocol, "Source protocol must not be null");
        Objects.requireNonNull(targetProtocol, "Target protocol must not be null");
        Objects.requireNonNull(requirement, "Conversion requirement must not be null");
        return conversionDefinitions.stream()
                .filter(Objects::nonNull)
                .filter(definition -> definition.matches(sourceProtocol, targetProtocol))
                .filter(ProtocolConversionDefinition::isEnabledForRouting)
                .filter(definition -> definition.capability().satisfies(requirement))
                .findFirst()
                .map(definition -> ConversionRoute.of(definition, sourceProtocol, targetProtocol));
    }

    private static Comparator<RouteCandidate> candidateOrdering() {
        return Comparator
                .comparing(RouteCandidate::priority)
                .thenComparing(candidate -> candidate.providerChannelId().value());
    }

    private static List<RouteFailure> collectFailures(List<RouteAttempt> attempts, RouteFailure latestFailure) {
        List<RouteFailure> failures = new ArrayList<>();
        for (RouteAttempt attempt : attempts) {
            if (attempt != null && attempt.failure() != null) {
                failures.add(attempt.failure());
            }
        }
        if (!failures.contains(latestFailure)) {
            failures.add(latestFailure);
        }
        return List.copyOf(failures);
    }

    private static Set<ProviderChannelId> collectAttemptedChannelIds(List<RouteAttempt> attempts, RouteFailure latestFailure) {
        Set<ProviderChannelId> attemptedChannelIds = new HashSet<>();
        for (RouteAttempt attempt : attempts) {
            if (attempt != null) {
                attemptedChannelIds.add(attempt.candidate().providerChannelId());
            }
        }
        attemptedChannelIds.add(latestFailure.providerChannelId());
        return attemptedChannelIds;
    }
}
