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
import java.time.Instant;
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
        UsageRecordStatus status = parseStatus(po);
        UsageErrorDiagnostic diagnostic = null;
        if (status == UsageRecordStatus.FAILED) {
            String message = po.getErrorMessage() == null || po.getErrorMessage().isBlank()
                    ? "Unknown invocation failure"
                    : po.getErrorMessage();
            diagnostic = UsageErrorDiagnostic.of(parseErrorType(po.getErrorType()), message, List.of());
        }
        Instant startedAt = safeStartedAt(po);
        Instant endedAt = safeEndedAt(po, startedAt);
        return UsageRecord.rehydrate(
                UsageRecordId.of(po.getId()),
                GatewayRequestId.of(safeText(po.getRequestId(), "unknown-" + po.getId())),
                UserAccountId.of(po.getUserAccountId()),
                ApiCredentialId.of(po.getApiCredentialId()),
                ModelName.of(safeText(po.getRequestedModel(), "unknown")),
                safeModelName(po.getUpstreamModel(), status),
                parseProtocol(po.getRequestProtocol(), ProtocolType.CLAUDE_MESSAGES),
                parseUpstreamProtocol(po.getUpstreamProtocol(), status),
                safeProviderChannelId(po.getProviderChannelId(), status),
                status,
                normalizedTokenBreakdown(
                        po.getInputTokens(),
                        po.getOutputTokens(),
                        po.getCacheCreationInputTokens(),
                        po.getCacheReadInputTokens(),
                        po.isUsageKnown()
                ),
                po.isStreaming(),
                startedAt,
                endedAt,
                UsageDuration.between(startedAt, endedAt),
                diagnostic,
                po.getCreatedTime() == null ? startedAt : po.getCreatedTime()
        );
    }

    private UsageRecordStatus parseStatus(UsageRecordPO po) {
        UsageRecordStatus status = null;
        if (po.getStatus() != null && !po.getStatus().isBlank()) {
            try {
                status = UsageRecordStatus.valueOf(po.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall through to infer from diagnostics.
            }
        }
        if (status == null) {
            status = po.getErrorType() != null || (po.getErrorMessage() != null && !po.getErrorMessage().isBlank())
                    ? UsageRecordStatus.FAILED
                    : UsageRecordStatus.SUCCESS;
        }
        if (status == UsageRecordStatus.SUCCESS && (po.getUpstreamModel() == null || po.getUpstreamModel().isBlank()
                || ProtocolType.parseExternal(po.getUpstreamProtocol()).isEmpty()
                || po.getProviderChannelId() == null)) {
            return UsageRecordStatus.FAILED;
        }
        return status;
    }

    private InvocationErrorType parseErrorType(String value) {
        if (value == null || value.isBlank()) {
            return InvocationErrorType.UPSTREAM_FAILED;
        }
        try {
            return InvocationErrorType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return InvocationErrorType.UPSTREAM_FAILED;
        }
    }

    private ProtocolType parseProtocol(String value, ProtocolType fallback) {
        return ProtocolType.parseExternal(value).orElse(fallback);
    }

    private ProtocolType parseUpstreamProtocol(String value, UsageRecordStatus status) {
        if (value == null || value.isBlank()) {
            return status == UsageRecordStatus.SUCCESS ? ProtocolType.CLAUDE_MESSAGES : null;
        }
        return parseProtocol(value, status == UsageRecordStatus.SUCCESS ? ProtocolType.CLAUDE_MESSAGES : null);
    }

    private ModelName safeModelName(String value, UsageRecordStatus status) {
        if (value == null || value.isBlank()) {
            return status == UsageRecordStatus.SUCCESS ? ModelName.of("unknown") : null;
        }
        return ModelName.of(value);
    }

    private ProviderChannelId safeProviderChannelId(Long value, UsageRecordStatus status) {
        if (value == null || value <= 0) {
            return null;
        }
        return ProviderChannelId.of(value);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Instant safeStartedAt(UsageRecordPO po) {
        if (po.getStartedTime() != null) {
            return po.getStartedTime();
        }
        if (po.getCreatedTime() != null) {
            return po.getCreatedTime();
        }
        if (po.getUpdatedTime() != null) {
            return po.getUpdatedTime();
        }
        return Instant.EPOCH;
    }

    private Instant safeEndedAt(UsageRecordPO po, Instant startedAt) {
        Instant endedAt = po.getEndedTime();
        if (endedAt == null && po.getDurationMillis() > 0) {
            endedAt = startedAt.plusMillis(po.getDurationMillis());
        }
        if (endedAt == null || endedAt.isBefore(startedAt)) {
            return startedAt;
        }
        return endedAt;
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
        return normalizedTokenBreakdown(
                summary.getInputTokens(),
                summary.getOutputTokens(),
                summary.getCacheCreationInputTokens(),
                summary.getCacheReadInputTokens(),
                summary.isUsageKnown()
        );
    }

    private UsageTokenBreakdown normalizedTokenBreakdown(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens,
            boolean usageKnown
    ) {
        long totalTokens = inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens;
        return UsageTokenBreakdown.of(
                inputTokens,
                outputTokens,
                cacheCreationInputTokens,
                cacheReadInputTokens,
                totalTokens,
                usageKnown
        );
    }
}
