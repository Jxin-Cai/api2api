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
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Routing policy domain service contract.
 */
public interface RoutingPolicyService {

    ConcurrentHashMap<String, AtomicInteger> LOAD_BALANCE_COUNTERS = new ConcurrentHashMap<>();

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

        List<RouteCandidate> selectedCandidates = prioritizeCandidates(request, candidates);
        return RoutePlan.of(request, selectedCandidates, now);
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
        List<ChannelModelSupport> modelSupports = channel.findModelSupports(request.requestedModel());
        if (modelSupports.isEmpty()) {
            return;
        }
        for (ProtocolType upstreamProtocol : candidateUpstreamProtocols(request, channel, modelSupports)) {
            Optional<ConversionRoute> conversionRoute = resolveRoute(
                    request.requestProtocol(),
                    upstreamProtocol,
                    request.requirement(),
                    conversionDefinitions
            );
            if (conversionRoute.isEmpty()) {
                continue;
            }
            for (ChannelModelSupport modelSupport : modelSupports) {
                if (modelSupport.upstreamProtocol() != upstreamProtocol) {
                    continue;
                }
                candidates.add(RouteCandidate.of(
                        channel.id(),
                        channel.name(),
                        modelSupport.requestedModel(),
                        modelSupport.upstreamModel(),
                        request.requestProtocol(),
                        upstreamProtocol,
                        modelSupport.priority(),
                        channel.routePriority(),
                        modelSupport.preferred(),
                        conversionRoute.get(),
                        ModelMappingResult.of(modelSupport.requestedModel(), modelSupport.upstreamModel())
                ));
            }
        }
    }

    private static Set<ProtocolType> candidateUpstreamProtocols(
            RoutingRequest request,
            ProviderChannel channel,
            List<ChannelModelSupport> modelSupports
    ) {
        Optional<ProtocolType> configuredUpstreamProtocol = channel.upstreamProtocolFor(request.requestProtocol());
        if (configuredUpstreamProtocol.isPresent()) {
            return Set.of(configuredUpstreamProtocol.get());
        }
        return modelSupports.stream()
                .map(ChannelModelSupport::upstreamProtocol)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
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

    private static List<RouteCandidate> prioritizeCandidates(RoutingRequest request, List<RouteCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<RouteCandidate> effectiveCandidates = candidates.stream().anyMatch(RouteCandidate::preferred)
                ? candidates.stream().filter(RouteCandidate::preferred).toList()
                : candidates;
        int maxPriority = effectiveCandidates.stream()
                .mapToInt(RouteCandidate::routePriority)
                .max()
                .orElse(0);
        List<RouteCandidate> topCandidates = effectiveCandidates.stream()
                .filter(candidate -> candidate.routePriority() == maxPriority)
                .sorted(candidateOrdering())
                .toList();
        List<RouteCandidate> remainingCandidates = effectiveCandidates.stream()
                .filter(candidate -> candidate.routePriority() != maxPriority)
                .sorted(candidateOrdering())
                .toList();
        List<RouteCandidate> fallbackCandidates = candidates.stream()
                .filter(candidate -> !effectiveCandidates.contains(candidate))
                .sorted(candidateOrdering())
                .toList();
        List<RouteCandidate> orderedCandidates = new ArrayList<>();
        orderedCandidates.addAll(rotateForLoadBalance(request, maxPriority, topCandidates));
        orderedCandidates.addAll(remainingCandidates);
        orderedCandidates.addAll(fallbackCandidates);
        return orderedCandidates;
    }

    private static List<RouteCandidate> rotateForLoadBalance(
            RoutingRequest request,
            int routePriority,
            List<RouteCandidate> candidates
    ) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        String key = request.requestProtocol().name() + ':'
                + request.requestedModel().value() + ':'
                + candidates.get(0).preferred() + ':'
                + routePriority;
        int startIndex = Math.floorMod(LOAD_BALANCE_COUNTERS
                .computeIfAbsent(key, ignored -> new AtomicInteger())
                .getAndIncrement(), candidates.size());
        List<RouteCandidate> rotated = new ArrayList<>(candidates.size());
        rotated.addAll(candidates.subList(startIndex, candidates.size()));
        rotated.addAll(candidates.subList(0, startIndex));
        return rotated;
    }

    private static Comparator<RouteCandidate> candidateOrdering() {
        return Comparator
                .comparing(RouteCandidate::preferred).reversed()
                .thenComparing(Comparator.comparingInt(RouteCandidate::routePriority).reversed())
                .thenComparing(RouteCandidate::priority)
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
