package com.api2api.domain.channel.event;

public interface ProviderChannelEventPublisher {

    /**
     * Publishes a provider channel status changed event after the aggregate status has been persisted successfully.
     * Consumers are expected to refresh routing candidate caches or trigger channel availability alerts.
     * Publication failure must not change the already persisted channel status fact; callers should handle retry or alerting.
     *
     * @param event provider channel status changed event
     */
    void publishStatusChanged(ProviderChannelStatusChangedEvent event);

    /**
     * Publishes a provider channel models changed event after the model support list has been persisted successfully.
     * Consumers are expected to refresh model presentation data and routing caches.
     * Publication failure must not roll back the already persisted model support change; callers should handle retry or alerting.
     *
     * @param event provider channel models changed event
     */
    void publishModelsChanged(ProviderChannelModelsChangedEvent event);
}
