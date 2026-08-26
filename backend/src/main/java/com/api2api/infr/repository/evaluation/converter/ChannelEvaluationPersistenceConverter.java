package com.api2api.infr.repository.evaluation.converter;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.model.ChannelEvaluationId;
import com.api2api.domain.evaluation.model.ChannelEvaluationSchedule;
import com.api2api.domain.evaluation.model.ChannelEvaluationScheduleId;
import com.api2api.domain.evaluation.model.EvaluationCron;
import com.api2api.domain.evaluation.model.EvaluationOutcome;
import com.api2api.domain.evaluation.model.EvaluationScore;
import com.api2api.domain.evaluation.model.EvaluationStatus;
import com.api2api.domain.evaluation.model.EvaluationTrigger;
import com.api2api.domain.evaluation.model.ProbeUpstreamFormat;
import com.api2api.domain.evaluation.repository.EvaluationScoreSummary;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.infr.repository.evaluation.po.ChannelEvaluationPO;
import com.api2api.infr.repository.evaluation.po.ChannelEvaluationSchedulePO;
import com.api2api.infr.repository.evaluation.po.EvaluationScoreSummaryPO;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts channel evaluation aggregates to persistence objects.
 */
@Mapper(config = MapStructConfig.class)
public interface ChannelEvaluationPersistenceConverter {

    @Mapping(target = "id", expression = "java(evaluation.id().value())")
    @Mapping(target = "providerChannelId", expression = "java(evaluation.providerChannelId().value())")
    @Mapping(target = "requestedModel", expression = "java(evaluation.requestedModel().value())")
    @Mapping(target = "upstreamFormat", expression = "java(evaluation.upstreamFormat().name())")
    @Mapping(target = "providerRunId", expression = "java(evaluation.providerRunId().orElse(null))")
    @Mapping(target = "status", expression = "java(evaluation.status().name())")
    @Mapping(target = "triggerType", expression = "java(evaluation.trigger().name())")
    @Mapping(target = "score", expression = "java(evaluation.outcome().map(outcome -> outcome.score().value()).orElse(null))")
    @Mapping(target = "detectedFamily", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::detectedFamily).orElse(null))")
    @Mapping(target = "detectedModel", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::detectedModel).orElse(null))")
    @Mapping(target = "detectedConfidence", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::detectedConfidence).orElse(null))")
    @Mapping(target = "familyMismatch", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::familyMismatch).orElse(null))")
    @Mapping(target = "channelSignature", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::channelSignature).orElse(null))")
    @Mapping(target = "reportUrl", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::reportUrl).orElse(null))")
    @Mapping(target = "passedProbeCount", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::passedProbeCount).orElse(null))")
    @Mapping(target = "warningProbeCount", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::warningProbeCount).orElse(null))")
    @Mapping(target = "failedProbeCount", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::failedProbeCount).orElse(null))")
    @Mapping(target = "totalInputTokens", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::totalInputTokens).orElse(null))")
    @Mapping(target = "totalOutputTokens", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::totalOutputTokens).orElse(null))")
    @Mapping(target = "errorMessage", expression = "java(evaluation.errorMessage().orElse(null))")
    @Mapping(target = "reportSummary", expression = "java(evaluation.outcome().map(com.api2api.domain.evaluation.model.EvaluationOutcome::reportSummary).orElse(null))")
    @Mapping(target = "requestedAt", expression = "java(evaluation.requestedAt())")
    @Mapping(target = "startedAt", expression = "java(evaluation.startedAt())")
    @Mapping(target = "completedAt", expression = "java(evaluation.completedAt())")
    @Mapping(target = "createdAt", expression = "java(evaluation.createdAt())")
    @Mapping(target = "updatedAt", expression = "java(evaluation.updatedAt())")
    ChannelEvaluationPO toPO(ChannelEvaluation evaluation);

    default ChannelEvaluation toDomain(ChannelEvaluationPO po) {
        EvaluationOutcome outcome = po.getScore() == null ? null : EvaluationOutcome.builder()
                .score(EvaluationScore.ofNormalized(po.getScore()))
                .detectedFamily(po.getDetectedFamily())
                .detectedModel(po.getDetectedModel())
                .detectedConfidence(po.getDetectedConfidence())
                .familyMismatch(po.getFamilyMismatch())
                .channelSignature(po.getChannelSignature())
                .reportUrl(po.getReportUrl())
                .passedProbeCount(po.getPassedProbeCount())
                .warningProbeCount(po.getWarningProbeCount())
                .failedProbeCount(po.getFailedProbeCount())
                .totalInputTokens(po.getTotalInputTokens())
                .totalOutputTokens(po.getTotalOutputTokens())
                .reportSummary(po.getReportSummary())
                .completedAt(po.getCompletedAt())
                .build();
        return ChannelEvaluation.rehydrate()
                .id(ChannelEvaluationId.of(po.getId()))
                .providerChannelId(ProviderChannelId.of(po.getProviderChannelId()))
                .requestedModel(ModelName.of(po.getRequestedModel()))
                .upstreamFormat(ProbeUpstreamFormat.valueOf(po.getUpstreamFormat()))
                .trigger(EvaluationTrigger.valueOf(po.getTriggerType()))
                .providerRunId(po.getProviderRunId())
                .status(EvaluationStatus.valueOf(po.getStatus()))
                .outcome(outcome)
                .errorMessage(po.getErrorMessage())
                .requestedAt(po.getRequestedAt())
                .startedAt(po.getStartedAt())
                .completedAt(po.getCompletedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    @Mapping(target = "id", expression = "java(schedule.id().value())")
    @Mapping(target = "providerChannelId", expression = "java(schedule.providerChannelId().value())")
    @Mapping(target = "cronExpression", expression = "java(schedule.cron().expression())")
    @Mapping(target = "zoneId", expression = "java(schedule.cron().zoneId().getId())")
    @Mapping(target = "models", expression = "java(encodeModels(schedule.models()))")
    @Mapping(target = "enabled", expression = "java(schedule.isEnabled())")
    @Mapping(target = "lastTriggeredAt", expression = "java(schedule.lastTriggeredAt().orElse(null))")
    @Mapping(target = "nextTriggerAt", expression = "java(schedule.nextTriggerAt().orElse(null))")
    @Mapping(target = "createdAt", expression = "java(schedule.createdAt())")
    @Mapping(target = "updatedAt", expression = "java(schedule.updatedAt())")
    ChannelEvaluationSchedulePO toSchedulePO(ChannelEvaluationSchedule schedule);

    default ChannelEvaluationSchedule toScheduleDomain(ChannelEvaluationSchedulePO po) {
        return ChannelEvaluationSchedule.rehydrate()
                .id(ChannelEvaluationScheduleId.of(po.getId()))
                .providerChannelId(ProviderChannelId.of(po.getProviderChannelId()))
                .cron(EvaluationCron.of(po.getCronExpression(), ZoneId.of(po.getZoneId())))
                .models(decodeModels(po.getModels()))
                .enabled(po.isEnabled())
                .lastTriggeredAt(po.getLastTriggeredAt())
                .nextTriggerAt(po.getNextTriggerAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    default EvaluationScoreSummary toSummary(EvaluationScoreSummaryPO po) {
        if (po == null) {
            return EvaluationScoreSummary.empty();
        }
        return new EvaluationScoreSummary(
                po.getTotalCount(),
                po.getScoredCount(),
                po.getFailedCount(),
                po.getAverageScore(),
                po.getMinScore(),
                po.getMaxScore()
        );
    }

    default String encodeModels(List<ModelName> models) {
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (ModelName model : models) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(model.value())).append('"');
            first = false;
        }
        return builder.append(']').toString();
    }

    default List<ModelName> decodeModels(String text) {
        if (text == null || text.isBlank() || "[]".equals(text.trim())) {
            return List.of();
        }
        String normalized = text.trim();
        List<ModelName> models = new ArrayList<>();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        String body = normalized.startsWith("[") && normalized.endsWith("]")
                ? normalized.substring(1, normalized.length() - 1).trim()
                : normalized;
        if (body.isEmpty()) {
            return List.of();
        }
        for (String item : body.split(",")) {
            String value = item.trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            value = value.replace("\\\"", "\"").replace("\\\\", "\\");
            if (!value.isBlank() && unique.add(value)) {
                models.add(ModelName.of(value));
            }
        }
        return models;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
