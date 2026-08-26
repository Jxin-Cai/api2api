package com.api2api.domain.evaluation.repository;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

/**
 * Filter, ordering and paging criteria for the evaluation history listing.
 *
 * @param providerChannelId channel whose history is listed, always required
 * @param requestedModel    optional model filter
 * @param status            optional status filter
 * @param from              inclusive lower bound on request time, optional
 * @param to                exclusive upper bound on request time, optional
 * @param sortField         column to order by
 * @param descending        {@code true} for descending order
 * @param limit             maximum rows to return
 * @param offset            rows to skip
 */
@Builder
public record EvaluationHistoryQuery(
        ProviderChannelId providerChannelId,
        ModelName requestedModel,
        EvaluationStatus status,
        Instant from,
        Instant to,
        EvaluationSortField sortField,
        boolean descending,
        int limit,
        int offset
) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    public EvaluationHistoryQuery {
        Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Evaluation history time range end must not precede its start");
        }
        sortField = sortField == null ? EvaluationSortField.REQUESTED_AT : sortField;
        limit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        offset = Math.max(offset, 0);
    }
}
