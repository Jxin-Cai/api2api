package com.api2api.domain.credential.event;

public interface ApiCredentialEventPublisher {

    /**
     * Publishes an API credential status changed event after the aggregate status has been persisted successfully.
     * Consumers may use the event to invalidate gateway authentication caches. Delivery failures must not change the
     * already saved API credential state and should be handled by application-level retry or alerting policies.
     *
     * @param event API credential status changed event
     */
    void publishStatusChanged(ApiCredentialStatusChangedEvent event);
}
