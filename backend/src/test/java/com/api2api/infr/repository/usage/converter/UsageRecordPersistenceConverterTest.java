package com.api2api.infr.repository.usage.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.UsageDuration;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.usage.model.UsageRecordStatus;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.repository.usage.po.UsageRecordPO;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class UsageRecordPersistenceConverterTest {

    private static final Instant STARTED_AT = Instant.parse("2026-07-08T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-07-08T10:00:01.250Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-08T10:00:02Z");

    private final UsageRecordPersistenceConverter converter = new UsageRecordPersistenceConverter();

    @ParameterizedTest
    @EnumSource(UsageRecordStatus.class)
    void test_preservesPersistedFields_when_rehydratingEachLifecycleState(UsageRecordStatus status) {
        // Arrange
        UsageRecordPO original = po();
        original.setUserAccountId(2L);
        original.setApiCredentialId(3L);
        original.setProviderChannelId(7L);
        original.setClientIp("192.0.2.1");
        original.setFirstTokenMillis(63L);
        original.setStreaming(true);
        original.setStatus(status.name());
        if (status == UsageRecordStatus.PENDING) {
            original.setUpstreamModel(null);
            original.setUpstreamProtocol(null);
            original.setProviderChannelId(null);
            original.setFirstTokenMillis(null);
            original.setEndedTime(STARTED_AT);
            original.setDurationMillis(0L);
            original.setInputTokens(4096L);
            original.setOutputTokens(0L);
            original.setCacheCreationInputTokens(0L);
            original.setCacheReadInputTokens(0L);
            original.setTotalTokens(4096L);
        } else if (status == UsageRecordStatus.FAILED) {
            original.setErrorType("UPSTREAM_FAILED");
            original.setErrorMessage("Upstream unavailable");
            original.setRouteFailuresJson("[]");
        }

        // Act
        UsageRecordPO restored = converter.toPO(converter.toDomain(original));

        // Assert
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, 125})
    void test_preservesFirstTokenLatency_when_latencyIsAbsentOrMeasured(Long latency) {
        // Arrange
        UsageRecordPO original = po();
        original.setFirstTokenMillis(latency);

        // Act
        UsageRecordPO restored = converter.toPO(converter.toDomain(original));

        // Assert
        assertThat(restored.getFirstTokenMillis()).isEqualTo(latency);
    }

    @Test
    void toDomainRecalculatesDurationFromTimestampsWhenPersistedDurationIsStale() {
        UsageRecordPO po = po();
        po.setDurationMillis(1L);

        UsageRecord record = converter.toDomain(po);

        assertThat(record.duration().millis()).isEqualTo(1250L);
    }

    @Test
    void toPOPersistsDomainDuration() {
        UsageRecord record = UsageRecord.rehydrate()
                .id(UsageRecordId.of(1L))
                .requestId(GatewayRequestId.of("request-1"))
                .userAccountId(UserAccountId.of(1L))
                .apiCredentialId(ApiCredentialId.of(1L))
                .requestedModel(ModelName.of("claude-sonnet"))
                .clientIp(null)
                .firstTokenMillis(null)
                .upstreamModel(ModelName.of("gpt-4.1"))
                .requestProtocol(ProtocolType.CLAUDE_MESSAGES)
                .upstreamProtocol(ProtocolType.OPENAI_RESPONSES)
                .providerChannelId(ProviderChannelId.of(1L))
                .status(UsageRecordStatus.SUCCESS)
                .tokenUsage(UsageTokenBreakdown.known(1L, 2L, 3L, 4L))
                .streaming(false)
                .startedAt(STARTED_AT)
                .endedAt(ENDED_AT)
                .duration(UsageDuration.between(STARTED_AT, ENDED_AT))
                .errorDiagnostic(null)
                .createdAt(CREATED_AT)
                .build();

        UsageRecordPO po = converter.toPO(record);

        assertThat(po.getDurationMillis()).isEqualTo(1250L);
    }

    @Test
    void test_recalculatesTotalTokens_when_persistedTotalIsInconsistent() {
        // Arrange
        UsageRecordPO po = po();
        po.setTotalTokens(999L);

        // Act
        UsageRecord record = converter.toDomain(po);

        // Assert
        assertThat(record.totalTokens()).isEqualTo(10L);
    }

    private UsageRecordPO po() {
        return UsageRecordPO.builder()
                .id(1L)
                .requestId("request-1")
                .userAccountId(1L)
                .apiCredentialId(1L)
                .requestedModel("claude-sonnet")
                .upstreamModel("gpt-4.1")
                .requestProtocol("CLAUDE_MESSAGES")
                .upstreamProtocol("OPENAI_RESPONSES")
                .providerChannelId(1L)
                .status("SUCCESS")
                .inputTokens(1L)
                .outputTokens(2L)
                .cacheCreationInputTokens(3L)
                .cacheReadInputTokens(4L)
                .totalTokens(10L)
                .usageKnown(true)
                .streaming(false)
                .startedTime(STARTED_AT)
                .endedTime(ENDED_AT)
                .durationMillis(1250L)
                .createdTime(CREATED_AT)
                .updatedTime(CREATED_AT)
                .deleted(false)
                .build();
    }
}
