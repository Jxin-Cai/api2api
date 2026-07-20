package com.api2api.domain.channel.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A channel model support item with optional model mapping and route priority.
 */
public class ChannelModelSupport {

    private final ChannelModelSupportId id;
    private final ModelName requestedModel;
    private ModelName upstreamModel;
    private final ProtocolType upstreamProtocol;
    private RoutePriority priority;
    private boolean preferred;
    private ChannelModelStatus status;
    private Instant rateLimitedAt;
    private Instant rateLimitResetAt;
    private final ModelSupportSource source;
    private final Instant createdAt;
    private Instant updatedAt;

    private ChannelModelSupport(
            ChannelModelSupportId id,
            ModelName requestedModel,
            ModelName upstreamModel,
            ProtocolType upstreamProtocol,
            RoutePriority priority,
            boolean preferred,
            ChannelModelStatus status,
            Instant rateLimitedAt,
            Instant rateLimitResetAt,
            ModelSupportSource source,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Channel model support id must not be null");
        this.requestedModel = Objects.requireNonNull(requestedModel, "Requested model must not be null");
        this.upstreamModel = Objects.requireNonNull(upstreamModel, "Upstream model must not be null");
        this.upstreamProtocol = Objects.requireNonNull(upstreamProtocol, "Upstream protocol must not be null");
        this.priority = Objects.requireNonNull(priority, "Route priority must not be null");
        this.preferred = preferred;
        this.status = Objects.requireNonNull(status, "Channel model status must not be null");
        this.rateLimitedAt = rateLimitedAt;
        this.rateLimitResetAt = rateLimitResetAt;
        this.source = Objects.requireNonNull(source, "Model support source must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
    }

    public static ChannelModelSupport create(
            ChannelModelSupportId id,
            ModelName requestedModel,
            ModelName upstreamModel,
            ProtocolType upstreamProtocol,
            RoutePriority priority,
            boolean preferred,
            ModelSupportSource source,
            Instant now
    ) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new ChannelModelSupport(
                id,
                requestedModel,
                upstreamModel,
                upstreamProtocol,
                priority,
                preferred,
                ChannelModelStatus.ENABLED,
                null,
                null,
                source,
                now,
                now
        );
    }

    public static ChannelModelSupport rehydrate(
            ChannelModelSupportId id,
            ModelName requestedModel,
            ModelName upstreamModel,
            ProtocolType upstreamProtocol,
            RoutePriority priority,
            boolean preferred,
            ChannelModelStatus status,
            Instant rateLimitedAt,
            Instant rateLimitResetAt,
            ModelSupportSource source,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ChannelModelSupport(
                id,
                requestedModel,
                upstreamModel,
                upstreamProtocol,
                priority,
                preferred,
                status,
                rateLimitedAt,
                rateLimitResetAt,
                source,
                createdAt,
                updatedAt
        );
    }

    public void changePriority(RoutePriority priority, Instant now) {
        Objects.requireNonNull(priority, "Route priority must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.priority.equals(priority)) {
            return;
        }
        this.priority = priority;
        this.updatedAt = now;
    }

    public void changePreferred(boolean preferred, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.preferred == preferred) {
            return;
        }
        this.preferred = preferred;
        this.updatedAt = now;
    }

    public void remapTo(ModelName upstreamModel, Instant now) {
        Objects.requireNonNull(upstreamModel, "Upstream model must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.upstreamModel.equals(upstreamModel)) {
            return;
        }
        this.upstreamModel = upstreamModel;
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (status == ChannelModelStatus.DISABLED) {
            return;
        }
        this.status = ChannelModelStatus.DISABLED;
        this.rateLimitedAt = null;
        this.rateLimitResetAt = null;
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (status == ChannelModelStatus.ENABLED) {
            return;
        }
        this.status = ChannelModelStatus.ENABLED;
        this.rateLimitedAt = null;
        this.rateLimitResetAt = null;
        this.updatedAt = now;
    }

    public boolean matches(ModelName requestedModel, ProtocolType upstreamProtocol) {
        return status == ChannelModelStatus.ENABLED
                && this.requestedModel.equals(requestedModel)
                && this.upstreamProtocol == upstreamProtocol;
    }

    public boolean hasModelMapping() {
        return !upstreamModel.equals(requestedModel);
    }

    public ModelMappingResult toModelMappingResult() {
        return ModelMappingResult.of(requestedModel, upstreamModel);
    }

    boolean hasSameCombinationAs(ChannelModelSupport other) {
        return other != null
                && requestedModel.equals(other.requestedModel)
                && upstreamModel.equals(other.upstreamModel)
                && upstreamProtocol == other.upstreamProtocol;
    }

    boolean hasCombination(ModelName requestedModel, ModelName upstreamModel, ProtocolType upstreamProtocol) {
        return this.requestedModel.equals(requestedModel)
                && this.upstreamModel.equals(upstreamModel)
                && this.upstreamProtocol == upstreamProtocol;
    }

    boolean isEnabled() {
        return status == ChannelModelStatus.ENABLED;
    }

    public ChannelModelSupportId id() {
        return id;
    }

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ModelName upstreamModel() {
        return upstreamModel;
    }

    public ProtocolType upstreamProtocol() {
        return upstreamProtocol;
    }

    public RoutePriority priority() {
        return priority;
    }

    public boolean preferred() {
        return preferred;
    }

    public ChannelModelStatus status() {
        return status;
    }

    public Instant rateLimitedAt() {
        return rateLimitedAt;
    }

    public Instant rateLimitResetAt() {
        return rateLimitResetAt;
    }

    public ModelSupportSource source() {
        return source;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
