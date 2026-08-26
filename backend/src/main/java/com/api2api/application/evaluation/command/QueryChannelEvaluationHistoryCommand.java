package com.api2api.application.evaluation.command;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import com.api2api.domain.evaluation.repository.EvaluationSortField;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for listing evaluation history of a provider channel.
 */
@Getter
public final class QueryChannelEvaluationHistoryCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    private final ModelName requestedModel;
    private final EvaluationStatus status;
    private final Instant from;
    private final Instant to;
    private final EvaluationSortField sortField;
    private final boolean descending;
    private final int limit;
    private final int offset;

    @Builder
    private QueryChannelEvaluationHistoryCommand(
            UserAccountId operatorUserId,
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
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.requestedModel = requestedModel;
        this.status = status;
        this.from = from;
        this.to = to;
        this.sortField = sortField;
        this.descending = descending;
        this.limit = limit;
        this.offset = offset;
    }
}
