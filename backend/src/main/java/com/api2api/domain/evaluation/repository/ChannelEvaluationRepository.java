package com.api2api.domain.evaluation.repository;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.model.ChannelEvaluationId;
import java.util.List;
import java.util.Optional;

public interface ChannelEvaluationRepository {

    /**
     * Creates or updates an evaluation run by id, persisting the score into its own columns so it can
     * be ranked and averaged without parsing the stored report.
     *
     * @param evaluation complete evaluation aggregate to save
     */
    void save(ChannelEvaluation evaluation);

    Optional<ChannelEvaluation> findById(ChannelEvaluationId id);

    /**
     * Lists evaluation runs matching the given filter, ordering and paging criteria.
     * No match is represented by an empty list, never {@code null}.
     *
     * @param query history query criteria
     * @return matching evaluation runs
     */
    List<ChannelEvaluation> findHistory(EvaluationHistoryQuery query);

    /** Counts every run matching the filters of the query, ignoring its paging. */
    long countHistory(EvaluationHistoryQuery query);

    /**
     * Aggregates score statistics over the window described by the query filters.
     * Returns {@link EvaluationScoreSummary#empty()} when nothing matches.
     *
     * @param query history query criteria whose paging and ordering are ignored
     * @return aggregated score statistics
     */
    EvaluationScoreSummary summarize(EvaluationHistoryQuery query);

    /**
     * Loads runs that have not reached a terminal status yet so the poller can refresh them.
     * Oldest requests come first so a backlog drains fairly.
     *
     * @param limit maximum number of runs to load
     * @return unfinished evaluation runs
     */
    List<ChannelEvaluation> findInFlight(int limit);

    /**
     * Hard deletes every evaluation run of a channel, used when the channel itself is removed.
     *
     * @param providerChannelId owning provider channel id
     * @return number of deleted runs
     */
    int deleteByProviderChannelId(ProviderChannelId providerChannelId);
}
