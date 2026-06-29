package com.api2api.infr.repository.usage.converter;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.gateway.model.InvocationErrorType;
import com.api2api.domain.usage.model.PageRequestSpec;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageDuration;
import com.api2api.domain.usage.model.UsageErrorDiagnostic;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.usage.model.UsageRecordStatus;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.repository.usage.po.UsageRecordPO;
import com.api2api.infr.repository.usage.po.UsageRecordQueryPO;
import com.api2api.infr.repository.usage.po.UsageTokenSummaryPO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Converts usage record domain objects and query filters to persistence models.
 */
@Component
public class UsageRecordPersistenceConverter {

    public UsageRecordPO toPO(UsageRecord usageRecord) {
        return UsageRecordPO.builder()
                .id(usageRecord.id().value())
                .requestId(usageRecord.requestId().value())
                .userAccountId(usageRecord.userAccountId().getValue())
                .apiCredentialId(usageRecord.apiCredentialId().value())
                .requestedModel(usageRecord.requestedModel().value())
                .upstreamModel(usageRecord.upstreamModel() == null ? null : usageRecord.upstreamModel().value())
                .requestProtocol(usageRecord.requestProtocol().name())
                .upstreamProtocol(usageRecord.upstreamProtocol() == null ? null : usageRecord.upstreamProtocol().name())
                .providerChannelId(usageRecord.providerChannelId() == null ? null : usageRecord.providerChannelId().value())
                .status(usageRecord.status().name())
                .inputTokens(usageRecord.tokenUsage().inputTokens())
                .outputTokens(usageRecord.tokenUsage().outputTokens())
                .cacheCreationInputTokens(usageRecord.tokenUsage().cacheCreationInputTokens())
                .cacheReadInputTokens(usageRecord.tokenUsage().cacheReadInputTokens())
                .totalTokens(usageRecord.tokenUsage().totalTokens())
                .usageKnown(usageRecord.tokenUsage().usageKnown())
                .streaming(usageRecord.streaming())
                .startedTime(usageRecord.startedAt())
                .endedTime(usageRecord.endedAt())
                .durationMillis(usageRecord.duration().millis())
                .errorType(usageRecord.errorDiagnostic() == null ? null : usageRecord.errorDiagnostic().errorType().name())
                .errorMessage(usageRecord.errorDiagnostic() == null ? null : usageRecord.errorDiagnostic().message())
                .routeFailuresJson(usageRecord.errorDiagnostic() == null ? null : usageRecord.errorDiagnostic().routeFailures().toString())
                .createdTime(usageRecord.createdAt())
                .updatedTime(usageRecord.createdAt())
                .deleted(false)
                .build();
    }

    public UsageRecord toDomain(UsageRecordPO po) {
        UsageRecordStatus status = UsageRecordStatus.valueOf(po.getStatus());
        UsageErrorDiagnostic diagnostic = null;
        if (status == UsageRecordStatus.FAILED) {
            InvocationErrorType errorType = po.getErrorType() == null
                    ? InvocationErrorType.UPSTREAM_FAILED
                    : InvocationErrorType.valueOf(po.getErrorType());
            String message = po.getErrorMessage() == null || po.getErrorMessage().isBlank()
                    ? "Unknown invocation failure"
                    : po.getErrorMessage();
            diagnostic = UsageErrorDiagnostic.of(errorType, message, List.of());
        }
        return UsageRecord.rehydrate(
                UsageRecordId.of(po.getId()),
                GatewayRequestId.of(po.getRequestId()),
                UserAccountId.of(po.getUserAccountId()),
                ApiCredentialId.of(po.getApiCredentialId()),
                ModelName.of(po.getRequestedModel()),
                po.getUpstreamModel() == null ? null : ModelName.of(po.getUpstreamModel()),
                ProtocolType.valueOf(po.getRequestProtocol()),
                po.getUpstreamProtocol() == null ? null : ProtocolType.valueOf(po.getUpstreamProtocol()),
                po.getProviderChannelId() == null ? null : ProviderChannelId.of(po.getProviderChannelId()),
                status,
                UsageTokenBreakdown.of(
                        po.getInputTokens(),
                        po.getOutputTokens(),
                        po.getCacheCreationInputTokens(),
                        po.getCacheReadInputTokens(),
                        po.getTotalTokens(),
                        po.isUsageKnown()
                ),
                po.isStreaming(),
                po.getStartedTime(),
                po.getEndedTime(),
                UsageDuration.of(po.getDurationMillis()),
                diagnostic,
                po.getCreatedTime()
        );
    }

    public UsageRecordQueryPO toQueryPO(UsageRecordFilter filter) {
        return UsageRecordQueryPO.builder()
                .userId(filter.userAccountId() == null ? null : filter.userAccountId().getValue())
                .apiCredentialId(filter.apiCredentialId() == null ? null : filter.apiCredentialId().value())
                .providerChannelId(filter.providerChannelId() == null ? null : filter.providerChannelId().value())
                .model(filter.requestedModel() == null ? null : filter.requestedModel().value())
                .protocol(filter.requestProtocol() == null ? null : filter.requestProtocol().name())
                .startTime(filter.timeRange().startInclusive())
                .endTime(filter.timeRange().endExclusive())
                .build();
    }

    public UsageRecordQueryPO toQueryPO(UsageRecordFilter filter, PageRequestSpec pageRequest) {
        UsageRecordQueryPO query = toQueryPO(filter);
        query.setPageNumber(pageRequest.page());
        query.setPageSize(pageRequest.size());
        return query;
    }

    public PagedUsageRecords toPage(List<UsageRecordPO> records, PageRequestSpec pageRequest, long total, UsageTokenSummaryPO summary) {
        return PagedUsageRecords.of(
                records.stream().map(this::toDomain).toList(),
                pageRequest.page(),
                pageRequest.size(),
                total,
                toTokenBreakdown(summary)
        );
    }

    public UsageTokenBreakdown toTokenBreakdown(UsageTokenSummaryPO summary) {
        if (summary == null) {
            return UsageTokenBreakdown.zeroKnown();
        }
        return UsageTokenBreakdown.of(
                summary.getInputTokens(),
                summary.getOutputTokens(),
                summary.getCacheCreationInputTokens(),
                summary.getCacheReadInputTokens(),
                summary.getTotalTokens(),
                summary.isUsageKnown()
        );
    }
}
