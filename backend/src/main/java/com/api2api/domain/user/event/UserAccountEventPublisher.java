package com.api2api.domain.user.event;

/**
 * Publisher interface for user account domain events.
 */
public interface UserAccountEventPublisher {

    /**
     * Publishes an event after a user account status change has been persisted successfully.
     * Consumers may invalidate login sessions or API key authorization caches according to the new status.
     * Delivery failures must not alter the persisted aggregate state and should be handled by the application layer.
     *
     * @param event user account status changed event to publish
     */
    void publishStatusChanged(UserAccountStatusChangedEvent event);

    /**
     * Publishes an event after a user account role change has been persisted successfully.
     * Consumers may refresh authorization contexts so subsequent permission checks use the new role.
     * Delivery failures must not roll back the confirmed role change and should be handled by the application layer.
     *
     * @param event user account role changed event to publish
     */
    void publishRoleChanged(UserAccountRoleChangedEvent event);
}
