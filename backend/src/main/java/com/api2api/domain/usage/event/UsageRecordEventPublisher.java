package com.api2api.domain.usage.event;

/**
 * Publishes usage record domain events after append-only usage facts are persisted.
 */
public interface UsageRecordEventPublisher {

    /**
     * Publish a usage record creation event.
     * Delivery failures must not roll back the already persisted usage fact.
     */
    void publishCreated(UsageRecordCreatedEvent event);
}
