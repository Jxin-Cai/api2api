package com.api2api.domain.evaluation.repository;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.ChannelEvaluationSchedule;
import com.api2api.domain.evaluation.model.ChannelEvaluationScheduleId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChannelEvaluationScheduleRepository {

    /**
     * Creates or updates the schedule of a channel. At most one schedule exists per channel, so
     * implementations should upsert on the owning channel id.
     *
     * @param schedule complete schedule aggregate to save
     */
    void save(ChannelEvaluationSchedule schedule);

    Optional<ChannelEvaluationSchedule> findById(ChannelEvaluationScheduleId id);

    Optional<ChannelEvaluationSchedule> findByProviderChannelId(ProviderChannelId providerChannelId);

    /**
     * Loads enabled schedules whose next trigger time has elapsed.
     * No due schedule is represented by an empty list, never {@code null}.
     *
     * @param now current time
     * @param limit maximum number of schedules to load
     * @return schedules ready to fire
     */
    List<ChannelEvaluationSchedule> findDue(Instant now, int limit);

    /**
     * Deletes the schedule of a channel, used when the channel itself is removed.
     *
     * @param providerChannelId owning provider channel id
     * @return number of deleted schedules
     */
    int deleteByProviderChannelId(ProviderChannelId providerChannelId);
}
