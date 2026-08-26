package com.api2api.ohs.http.admin.converter;

import com.api2api.application.evaluation.command.LoadChannelEvaluationScheduleCommand;
import com.api2api.application.evaluation.command.QueryChannelEvaluationHistoryCommand;
import com.api2api.application.evaluation.command.SubmitChannelEvaluationCommand;
import com.api2api.application.evaluation.command.UpsertChannelEvaluationScheduleCommand;
import com.api2api.application.evaluation.dto.ChannelEvaluationHistoryPage;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.model.ChannelEvaluationSchedule;
import com.api2api.domain.evaluation.model.EvaluationCron;
import com.api2api.domain.evaluation.model.EvaluationOutcome;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import com.api2api.domain.evaluation.repository.EvaluationScoreSummary;
import com.api2api.domain.evaluation.repository.EvaluationSortField;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.admin.dto.AdminSubmitChannelEvaluationRequest;
import com.api2api.ohs.http.admin.dto.AdminUpsertChannelEvaluationScheduleRequest;
import com.api2api.ohs.http.admin.dto.ChannelEvaluationHistoryResponse;
import com.api2api.ohs.http.admin.dto.ChannelEvaluationResponse;
import com.api2api.ohs.http.admin.dto.ChannelEvaluationScheduleResponse;
import com.api2api.ohs.http.admin.dto.ChannelEvaluationSubmitResponse;
import com.api2api.ohs.http.admin.dto.QueryChannelEvaluationHistoryRequest;
import com.api2api.ohs.http.converter.MapStructConfig;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Converts channel evaluation HTTP models to application commands and responses.
 */
@Mapper(config = MapStructConfig.class)
public interface ChannelEvaluationHttpConverter {

    default SubmitChannelEvaluationCommand toSubmitCommand(
            AdminSubmitChannelEvaluationRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        List<String> models = request == null ? List.of() : defaultList(request.getModels());
        return SubmitChannelEvaluationCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .models(toModelNames(models))
                .build();
    }

    default QueryChannelEvaluationHistoryCommand toHistoryCommand(
            QueryChannelEvaluationHistoryRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        QueryChannelEvaluationHistoryRequest query = request == null
                ? new QueryChannelEvaluationHistoryRequest()
                : request;
        return QueryChannelEvaluationHistoryCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .requestedModel(toModelName(query.getRequestedModel()))
                .status(EvaluationStatus.parse(query.getStatus()).orElse(null))
                .from(query.getFrom())
                .to(query.getTo())
                .sortField(EvaluationSortField.parse(query.getSortField()).orElse(EvaluationSortField.REQUESTED_AT))
                .descending(query.getDescending() == null || query.getDescending())
                .limit(query.getLimit() == null ? 0 : query.getLimit())
                .offset(query.getOffset() == null ? 0 : query.getOffset())
                .build();
    }

    default LoadChannelEvaluationScheduleCommand toLoadScheduleCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        return LoadChannelEvaluationScheduleCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .build();
    }

    default UpsertChannelEvaluationScheduleCommand toUpsertScheduleCommand(
            AdminUpsertChannelEvaluationScheduleRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        ZoneId zoneId = toZoneId(request.getZoneId());
        return UpsertChannelEvaluationScheduleCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .cron(EvaluationCron.of(request.getCronExpression(), zoneId))
                .models(toModelNames(defaultList(request.getModels())))
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();
    }

    default ChannelEvaluationSubmitResponse toSubmitResponse(List<ChannelEvaluation> evaluations) {
        return ChannelEvaluationSubmitResponse.builder()
                .evaluations(evaluations.stream().map(this::toResponse).toList())
                .build();
    }

    default ChannelEvaluationHistoryResponse toHistoryResponse(ChannelEvaluationHistoryPage page) {
        EvaluationScoreSummary summary = page.summary();
        return ChannelEvaluationHistoryResponse.builder()
                .evaluations(page.evaluations().stream().map(this::toResponse).toList())
                .summary(ChannelEvaluationHistoryResponse.ChannelEvaluationScoreSummaryResponse.builder()
                        .totalCount(summary.totalCount())
                        .scoredCount(summary.scoredCount())
                        .failedCount(summary.failedCount())
                        .averageScore(summary.averageScore())
                        .minScore(summary.minScore())
                        .maxScore(summary.maxScore())
                        .build())
                .totalElements(page.totalElements())
                .limit(page.limit())
                .offset(page.offset())
                .build();
    }

    default ChannelEvaluationResponse toResponse(ChannelEvaluation evaluation) {
        EvaluationOutcome outcome = evaluation.outcome().orElse(null);
        return ChannelEvaluationResponse.builder()
                .id(evaluation.id().value())
                .providerChannelId(evaluation.providerChannelId().value())
                .requestedModel(evaluation.requestedModel().value())
                .upstreamFormat(evaluation.upstreamFormat().name())
                .providerRunId(evaluation.providerRunId().orElse(null))
                .status(evaluation.status().name())
                .trigger(evaluation.trigger().name())
                .score(outcome == null ? null : outcome.score().value())
                .detectedFamily(outcome == null ? null : outcome.detectedFamily())
                .detectedModel(outcome == null ? null : outcome.detectedModel())
                .detectedConfidence(outcome == null ? null : outcome.detectedConfidence())
                .familyMismatch(outcome == null ? null : outcome.familyMismatch())
                .channelSignature(outcome == null ? null : outcome.channelSignature())
                .reportUrl(outcome == null ? null : outcome.reportUrl())
                .passedProbeCount(outcome == null ? null : outcome.passedProbeCount())
                .warningProbeCount(outcome == null ? null : outcome.warningProbeCount())
                .failedProbeCount(outcome == null ? null : outcome.failedProbeCount())
                .totalInputTokens(outcome == null ? null : outcome.totalInputTokens())
                .totalOutputTokens(outcome == null ? null : outcome.totalOutputTokens())
                .errorMessage(evaluation.errorMessage().orElse(null))
                .reportSummary(outcome == null ? null : outcome.reportSummary())
                .requestedAt(toEpochMilli(evaluation.requestedAt()))
                .startedAt(toEpochMilli(evaluation.startedAt()))
                .completedAt(toEpochMilli(evaluation.completedAt()))
                .createdAt(toEpochMilli(evaluation.createdAt()))
                .updatedAt(toEpochMilli(evaluation.updatedAt()))
                .build();
    }

    default ChannelEvaluationScheduleResponse toScheduleResponse(ChannelEvaluationSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return ChannelEvaluationScheduleResponse.builder()
                .id(schedule.id().value())
                .providerChannelId(schedule.providerChannelId().value())
                .cronExpression(schedule.cron().expression())
                .zoneId(schedule.cron().zoneId().getId())
                .models(schedule.models().stream().map(ModelName::value).toList())
                .enabled(schedule.isEnabled())
                .lastTriggeredAt(toEpochMilli(schedule.lastTriggeredAt().orElse(null)))
                .nextTriggerAt(toEpochMilli(schedule.nextTriggerAt().orElse(null)))
                .createdAt(toEpochMilli(schedule.createdAt()))
                .updatedAt(toEpochMilli(schedule.updatedAt()))
                .build();
    }

    default Long toEpochMilli(java.time.Instant value) {
        return value == null ? null : value.toEpochMilli();
    }

    private static List<ModelName> toModelNames(List<String> models) {
        List<ModelName> names = new ArrayList<>();
        for (String model : models) {
            if (model != null && !model.isBlank()) {
                names.add(ModelName.of(model));
            }
        }
        return names;
    }

    private static ModelName toModelName(String value) {
        return value == null || value.isBlank() ? null : ModelName.of(value);
    }

    private static List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static ZoneId toZoneId(String value) {
        if (value == null || value.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(value.trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Evaluation cron zone is invalid: " + value, exception);
        }
    }
}
