package com.api2api.domain.channel.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregate root representing an upstream provider channel and its routable model capabilities.
 */
public class ProviderChannel {

    private final ProviderChannelId id;
    private ProviderChannelName name;
    private ProviderHost host;
    private ProviderKeyRef keyRef;
    private int routePriority;
    private Set<ProtocolType> supportedProtocols;
    private List<ChannelModelSupport> supportedModels;
    private ProviderChannelStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private ProviderChannel(
            ProviderChannelId id,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            int routePriority,
            Set<ProtocolType> supportedProtocols,
            List<ChannelModelSupport> supportedModels,
            ProviderChannelStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Provider channel id must not be null");
        this.name = Objects.requireNonNull(name, "Provider channel name must not be null");
        this.host = Objects.requireNonNull(host, "Provider host must not be null");
        this.keyRef = Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        this.routePriority = routePriority;
        this.supportedProtocols = normalizeProtocols(supportedProtocols);
        this.supportedModels = normalizeModels(supportedModels);
        this.status = Objects.requireNonNull(status, "Provider channel status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
    }

    public static ProviderChannel create(
            ProviderChannelId id,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            int routePriority,
            Set<ProtocolType> supportedProtocols,
            Instant now
    ) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new ProviderChannel(
                id,
                name,
                host,
                keyRef,
                routePriority,
                supportedProtocols,
                List.of(),
                ProviderChannelStatus.ENABLED,
                now,
                now
        );
    }

    public static ProviderChannel rehydrate(
            ProviderChannelId id,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            int routePriority,
            Set<ProtocolType> supportedProtocols,
            List<ChannelModelSupport> supportedModels,
            ProviderChannelStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ProviderChannel(
                id,
                name,
                host,
                keyRef,
                routePriority,
                supportedProtocols,
                supportedModels,
                status,
                createdAt,
                updatedAt
        );
    }

    public void rename(ProviderChannelName name, Instant now) {
        Objects.requireNonNull(name, "Channel name must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.name.equals(name)) {
            return;
        }
        this.name = name;
        this.updatedAt = now;
    }

    public void updateEndpoint(ProviderHost host, ProviderKeyRef keyRef, Instant now) {
        Objects.requireNonNull(host, "Provider host must not be null");
        Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.host.equals(host) && this.keyRef.equals(keyRef)) {
            return;
        }
        this.host = host;
        this.keyRef = keyRef;
        this.updatedAt = now;
    }

    public void changeRoutePriority(int routePriority, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.routePriority == routePriority) {
            return;
        }
        this.routePriority = routePriority;
        this.updatedAt = now;
    }

    public void replaceSupportedProtocols(Set<ProtocolType> protocols, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        Set<ProtocolType> normalizedProtocols = normalizeProtocols(protocols);
        if (this.supportedProtocols.equals(normalizedProtocols)) {
            return;
        }
        this.supportedProtocols = normalizedProtocols;
        this.updatedAt = now;
    }

    public void addOrUpdateModel(ChannelModelSupport modelSupport, Instant now) {
        Objects.requireNonNull(modelSupport, "Channel model support must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        ensureProtocolSupported(modelSupport.upstreamProtocol());

        List<ChannelModelSupport> changedModels = new ArrayList<>(supportedModels);
        Optional<Integer> existingIndex = findSameCombinationIndex(modelSupport, changedModels);
        if (existingIndex.isPresent()) {
            changedModels.set(existingIndex.get(), modelSupport);
        } else {
            changedModels.add(modelSupport);
        }
        this.supportedModels = normalizeModels(changedModels);
        this.updatedAt = now;
    }

    public void replaceModels(List<ChannelModelSupport> modelSupports, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        List<ChannelModelSupport> normalizedModels = normalizeModels(modelSupports);
        normalizedModels.forEach(modelSupport -> ensureProtocolSupported(modelSupport.upstreamProtocol()));
        if (this.supportedModels.equals(normalizedModels)) {
            return;
        }
        this.supportedModels = normalizedModels;
        this.updatedAt = now;
    }

    public void removeModel(ModelName requestedModel, ProtocolType upstreamProtocol, Instant now) {
        Objects.requireNonNull(requestedModel, "Requested model must not be null");
        Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null");
        Objects.requireNonNull(now, "Current time must not be null");

        List<ChannelModelSupport> changedModels = supportedModels.stream()
                .filter(modelSupport -> !modelSupport.requestedModel().equals(requestedModel)
                        || modelSupport.upstreamProtocol() != upstreamProtocol)
                .toList();
        if (changedModels.size() == supportedModels.size()) {
            return;
        }
        this.supportedModels = new ArrayList<>(changedModels);
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (status == ProviderChannelStatus.ENABLED) {
            return;
        }
        this.status = ProviderChannelStatus.ENABLED;
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (status == ProviderChannelStatus.DISABLED) {
            return;
        }
        this.status = ProviderChannelStatus.DISABLED;
        this.updatedAt = now;
    }

    public void markDegraded(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (status == ProviderChannelStatus.DEGRADED) {
            return;
        }
        this.status = ProviderChannelStatus.DEGRADED;
        this.updatedAt = now;
    }

    public boolean isEnabledForRouting() {
        return status == ProviderChannelStatus.ENABLED;
    }

    public boolean supportsProtocol(ProtocolType protocol) {
        Objects.requireNonNull(protocol, "Protocol must not be null");
        return supportedProtocols.contains(protocol);
    }

    public boolean supportsModel(ModelName requestedModel, ProtocolType upstreamProtocol) {
        return findModelSupport(requestedModel, upstreamProtocol).isPresent();
    }

    public Optional<ChannelModelSupport> findModelSupport(ModelName requestedModel, ProtocolType upstreamProtocol) {
        Objects.requireNonNull(requestedModel, "Requested model must not be null");
        Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null");
        if (!supportsProtocol(upstreamProtocol)) {
            return Optional.empty();
        }
        return supportedModels.stream()
                .filter(modelSupport -> modelSupport.matches(requestedModel, upstreamProtocol))
                .min(modelOrdering());
    }

    public List<ChannelModelSupport> findModelSupports(ModelName requestedModel) {
        Objects.requireNonNull(requestedModel, "Requested model must not be null");
        return supportedModels.stream()
                .filter(ChannelModelSupport::isEnabled)
                .filter(modelSupport -> modelSupport.requestedModel().equals(requestedModel))
                .filter(modelSupport -> supportsProtocol(modelSupport.upstreamProtocol()))
                .sorted(modelOrdering())
                .toList();
    }

    public RoutePriority priorityFor(ModelName requestedModel, ProtocolType upstreamProtocol) {
        return findModelSupport(requestedModel, upstreamProtocol)
                .map(ChannelModelSupport::priority)
                .orElseThrow(() -> new IllegalStateException("Provider channel does not support requested model and protocol"));
    }

    private Optional<Integer> findSameCombinationIndex(ChannelModelSupport modelSupport, List<ChannelModelSupport> modelSupports) {
        for (int index = 0; index < modelSupports.size(); index++) {
            if (modelSupports.get(index).hasSameCombinationAs(modelSupport)) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    private void ensureProtocolSupported(ProtocolType protocol) {
        if (!supportsProtocol(protocol)) {
            throw new IllegalArgumentException("Model support protocol must be contained in provider channel supported protocols");
        }
    }

    private static Set<ProtocolType> normalizeProtocols(Set<ProtocolType> protocols) {
        Objects.requireNonNull(protocols, "Supported protocols must not be null");
        if (protocols.isEmpty()) {
            throw new IllegalArgumentException("Provider channel must support at least one protocol");
        }
        Set<ProtocolType> normalizedProtocols = new LinkedHashSet<>();
        for (ProtocolType protocol : protocols) {
            normalizedProtocols.add(Objects.requireNonNull(protocol, "Supported protocol must not be null"));
        }
        return normalizedProtocols;
    }

    private static List<ChannelModelSupport> normalizeModels(List<ChannelModelSupport> modelSupports) {
        Objects.requireNonNull(modelSupports, "Supported models must not be null");
        List<ChannelModelSupport> normalizedModels = new ArrayList<>();
        Set<ModelCombination> combinations = new HashSet<>();
        for (ChannelModelSupport modelSupport : modelSupports) {
            ChannelModelSupport requiredModelSupport = Objects.requireNonNull(
                    modelSupport,
                    "Channel model support must not be null"
            );
            ModelCombination combination = ModelCombination.from(requiredModelSupport);
            if (!combinations.add(combination)) {
                throw new IllegalArgumentException("Duplicated channel model support combination");
            }
            normalizedModels.add(requiredModelSupport);
        }
        return normalizedModels;
    }

    private static Comparator<ChannelModelSupport> modelOrdering() {
        return Comparator
                .comparing(ChannelModelSupport::priority)
                .thenComparing(modelSupport -> modelSupport.createdAt());
    }

    public ProviderChannelId id() {
        return id;
    }

    public ProviderChannelName name() {
        return name;
    }

    public ProviderHost host() {
        return host;
    }

    public ProviderKeyRef keyRef() {
        return keyRef;
    }

    public int routePriority() {
        return routePriority;
    }

    public Set<ProtocolType> supportedProtocols() {
        return Set.copyOf(supportedProtocols);
    }

    public List<ChannelModelSupport> supportedModels() {
        return List.copyOf(supportedModels);
    }

    public ProviderChannelStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private record ModelCombination(ModelName requestedModel, ModelName upstreamModel, ProtocolType upstreamProtocol) {

        private static ModelCombination from(ChannelModelSupport modelSupport) {
            return new ModelCombination(
                    modelSupport.requestedModel(),
                    modelSupport.upstreamModel(),
                    modelSupport.upstreamProtocol()
            );
        }
    }
}
