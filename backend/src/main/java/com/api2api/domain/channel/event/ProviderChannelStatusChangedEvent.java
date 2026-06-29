package com.api2api.domain.channel.event;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event emitted when a provider channel status changes.
 */
public final class ProviderChannelStatusChangedEvent {

    private final ProviderChannelId providerChannelId;
    private final ProviderChannelStatus previousStatus;
    private final ProviderChannelStatus currentStatus;
    private final Instant occurredAt;

    private ProviderChannelStatusChangedEvent(
            ProviderChannelId providerChannelId,
            ProviderChannelStatus previousStatus,
            ProviderChannelStatus currentStatus,
            Instant occurredAt
    ) {
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.previousStatus = Objects.requireNonNull(previousStatus, "Previous status must not be null");
        this.currentStatus = Objects.requireNonNull(currentStatus, "Current status must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred time must not be null");
    }

    public static ProviderChannelStatusChangedEvent of(
            ProviderChannelId providerChannelId,
            ProviderChannelStatus previousStatus,
            ProviderChannelStatus currentStatus,
            Instant occurredAt
    ) {
        if (previousStatus == currentStatus) {
            throw new IllegalArgumentException("Status changed event requires different previous and current statuses");
        }
        return new ProviderChannelStatusChangedEvent(providerChannelId, previousStatus, currentStatus, occurredAt);
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public ProviderChannelStatus previousStatus() {
        return previousStatus;
    }

    public ProviderChannelStatus currentStatus() {
        return currentStatus;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
