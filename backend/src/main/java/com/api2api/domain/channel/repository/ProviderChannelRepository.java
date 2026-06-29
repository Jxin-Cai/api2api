package com.api2api.domain.channel.repository;

import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;

import java.util.List;
import java.util.Optional;

public interface ProviderChannelRepository {

    /**
     * Saves a complete provider channel aggregate.
     * Implementations should create or update by id, persist supported protocols and model support children as one aggregate,
     * and never persist upstream secret plaintext.
     * Duplicate channel names, duplicate model support combinations or persistence failures should be reported as business failures.
     *
     * @param providerChannel complete provider channel aggregate to save
     */
    void save(ProviderChannel providerChannel);

    /**
     * Loads a complete provider channel aggregate by id, including protocol set, model supports and status.
     * Returns {@link Optional#empty()} when no channel exists for the id.
     * Invalid ids should be rejected by the {@link ProviderChannelId} value object before repository access.
     *
     * @param id provider channel id
     * @return optional complete provider channel aggregate
     */
    Optional<ProviderChannel> findById(ProviderChannelId id);

    /**
     * Loads all provider channel aggregates for administrative management.
     * Implementations should return a stable order such as created time descending or channel name order,
     * and include protocol and model support summaries required by the aggregate.
     * No data should be represented by an empty list, never {@code null}.
     *
     * @return all provider channel aggregates
     */
    List<ProviderChannel> findAll();

    /**
     * Loads all provider channel aggregates that may participate in routing.
     * Implementations should only return ENABLED channels and preload protocols plus enabled model supports
     * so routing does not depend on lazy loading.
     * No candidate should be represented by an empty list, never {@code null}.
     *
     * @return enabled provider channels ready for routing decisions
     */
    List<ProviderChannel> findEnabledForRouting();
}
