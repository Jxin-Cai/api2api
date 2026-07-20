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
    private ProviderModelsPath modelsPath;
    private int routePriority;
    private Set<ChannelProtocolMapping> protocolMappings;
    private List<ChannelModelSupport> supportedModels;
    private ProviderChannelStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private ProviderChannel(
            ProviderChannelId id,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            ProviderModelsPath modelsPath,
            int routePriority,
            Set<ChannelProtocolMapping> protocolMappings,
            List<ChannelModelSupport> supportedModels,
            ProviderChannelStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Provider channel id must not be null");
        this.name = Objects.requireNonNull(name, "Provider channel name must not be null");
        this.host = Objects.requireNonNull(host, "Provider host must not be null");
        this.keyRef = Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        this.modelsPath = Objects.requireNonNull(modelsPath, "Provider models path must not be null");
        this.routePriority = routePriority;
        this.protocolMappings = normalizeProtocolMappings(protocolMappings);
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
        return create(id, name, host, keyRef, ProviderModelsPath.DEFAULT, routePriority, identityMappings(supportedProtocols), now);
    }

    public static ProviderChannel create(
            ProviderChannelId id,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            ProviderModelsPath modelsPath,
            int routePriority,
            Set<ChannelProtocolMapping> protocolMappings,
            Instant now
    ) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new ProviderChannel(
                id,
                name,
                host,
                keyRef,
                modelsPath,
                routePriority,
                protocolMappings,
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
        return rehydrate(
                id,
                name,
                host,
                keyRef,
                ProviderModelsPath.DEFAULT,
                routePriority,
                identityMappings(supportedProtocols),
                supportedModels,
                status,
                createdAt,
                updatedAt
        );
    }

    public static ProviderChannel rehydrate(
            ProviderChannelId id,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            ProviderModelsPath modelsPath,
            int routePriority,
            Set<ChannelProtocolMapping> protocolMappings,
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
                modelsPath,
                routePriority,
                protocolMappings,
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

    public void changeModelsPath(ProviderModelsPath modelsPath, Instant now) {
        Objects.requireNonNull(modelsPath, "Provider models path must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.modelsPath.equals(modelsPath)) {
            return;
        }
        this.modelsPath = modelsPath;
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
        replaceProtocolMappings(identityMappings(protocols), now);
    }

    public void replaceProtocolMappings(Set<ChannelProtocolMapping> mappings, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        Set<ChannelProtocolMapping> normalizedMappings = normalizeProtocolMappings(mappings);
        if (this.protocolMappings.equals(normalizedMappings)) {
            return;
        }
        this.protocolMappings = normalizedMappings;
        this.updatedAt = now;
    }

    public void addOrUpdateModel(ChannelModelSupport modelSupport, Instant now) {
        Objects.requireNonNull(modelSupport, "Channel model support must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        ensureUpstreamProtocolSupported(modelSupport.upstreamProtocol());

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
        normalizedModels.forEach(modelSupport -> ensureUpstreamProtocolSupported(modelSupport.upstreamProtocol()));
        if (this.supportedModels.equals(normalizedModels)) {
            return;
        }
        this.supportedModels = normalizedModels;
        this.updatedAt = now;
    }

    public void enableModel(ChannelModelSupportId modelId, Instant now) {
        Objects.requireNonNull(modelId, "Channel model support id must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        ChannelModelSupport model = findModelById(modelId);
        model.enable(now);
        this.updatedAt = now;
    }

    public void disableModel(ChannelModelSupportId modelId, Instant now) {
        Objects.requireNonNull(modelId, "Channel model support id must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        ChannelModelSupport model = findModelById(modelId);
        model.disable(now);
        this.updatedAt = now;
    }

    private ChannelModelSupport findModelById(ChannelModelSupportId modelId) {
        return supportedModels.stream()
                .filter(m -> m.id().equals(modelId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Channel model support not found: " + modelId.value()));
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
        boolean restoredRateLimitedModel = false;
        for (ChannelModelSupport model : supportedModels) {
            if (model.status() == ChannelModelStatus.RATE_LIMITED) {
                model.enable(now);
                restoredRateLimitedModel = true;
            }
        }
        if (status == ProviderChannelStatus.ENABLED && !restoredRateLimitedModel) {
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
        return supportsRequestProtocol(protocol);
    }

    public boolean supportsRequestProtocol(ProtocolType protocol) {
        Objects.requireNonNull(protocol, "Protocol must not be null");
        return supportedProtocols().contains(protocol);
    }

    public boolean supportsUpstreamProtocol(ProtocolType protocol) {
        Objects.requireNonNull(protocol, "Protocol must not be null");
        return upstreamProtocols().contains(protocol);
    }

    public Optional<ProtocolType> upstreamProtocolFor(ProtocolType requestProtocol) {
        Objects.requireNonNull(requestProtocol, "Request protocol must not be null");
        return protocolMappings.stream()
                .filter(mapping -> mapping.requestProtocol() == requestProtocol)
                .map(ChannelProtocolMapping::upstreamProtocol)
                .findFirst();
    }

    public Set<ProtocolType> upstreamProtocols() {
        return protocolMappings.stream()
                .map(ChannelProtocolMapping::upstreamProtocol)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean supportsModel(ModelName requestedModel, ProtocolType upstreamProtocol) {
        return findModelSupport(requestedModel, upstreamProtocol).isPresent();
    }

    public Optional<ChannelModelSupport> findModelSupport(ModelName requestedModel, ProtocolType upstreamProtocol) {
        Objects.requireNonNull(requestedModel, "Requested model must not be null");
        Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null");
        if (!supportsUpstreamProtocol(upstreamProtocol)) {
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
                .filter(modelSupport -> supportsUpstreamProtocol(modelSupport.upstreamProtocol()))
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
            ChannelModelSupport existing = modelSupports.get(index);
            if (existing.id().equals(modelSupport.id()) || existing.hasSameCombinationAs(modelSupport)) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    private void ensureUpstreamProtocolSupported(ProtocolType protocol) {
        ensureUpstreamProtocolSupported(protocol, protocolMappings);
    }

    private static void ensureUpstreamProtocolSupported(ProtocolType protocol, Set<ChannelProtocolMapping> mappings) {
        boolean supported = mappings.stream().anyMatch(mapping -> mapping.upstreamProtocol() == protocol);
        if (!supported) {
            throw new IllegalArgumentException("Model upstream protocol must be contained in provider channel protocol mappings");
        }
    }

    private static Set<ChannelProtocolMapping> identityMappings(Set<ProtocolType> protocols) {
        Objects.requireNonNull(protocols, "Supported protocols must not be null");
        return protocols.stream()
                .map(protocol -> ChannelProtocolMapping.of(protocol, protocol))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<ChannelProtocolMapping> normalizeProtocolMappings(Set<ChannelProtocolMapping> mappings) {
        Objects.requireNonNull(mappings, "Protocol mappings must not be null");
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("Provider channel must contain at least one protocol mapping");
        }
        Set<ChannelProtocolMapping> normalized = new LinkedHashSet<>();
        Set<ProtocolType> requestProtocols = new HashSet<>();
        for (ChannelProtocolMapping mapping : mappings) {
            ChannelProtocolMapping requiredMapping = Objects.requireNonNull(mapping, "Protocol mapping must not be null");
            if (!requestProtocols.add(requiredMapping.requestProtocol())) {
                throw new IllegalArgumentException("Duplicated request protocol mapping");
            }
            normalized.add(requiredMapping);
        }
        return normalized;
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

    public ProviderModelsPath modelsPath() {
        return modelsPath;
    }

    public int routePriority() {
        return routePriority;
    }

    public Set<ProtocolType> supportedProtocols() {
        return protocolMappings.stream()
                .map(ChannelProtocolMapping::requestProtocol)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<ChannelProtocolMapping> protocolMappings() {
        return Set.copyOf(protocolMappings);
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
