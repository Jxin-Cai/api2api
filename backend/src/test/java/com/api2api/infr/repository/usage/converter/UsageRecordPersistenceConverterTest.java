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

class UsageRecordPersistenceConverterTest {

    private static final Instant STARTED_AT = Instant.parse("2026-07-08T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-07-08T10:00:01.250Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-08T10:00:02Z");

    private final UsageRecordPersistenceConverter converter = new UsageRecordPersistenceConverter();

    @Test
    void toDomainRecalculatesDurationFromTimestampsWhenPersistedDurationIsStale() {
        UsageRecordPO po = po();
        po.setDurationMillis(1L);

        UsageRecord record = converter.toDomain(po);

        assertThat(record.duration().millis()).isEqualTo(1250L);
    }

    @Test
    void toPOPersistsDomainDuration() {
        UsageRecord record = UsageRecord.rehydrate(
                UsageRecordId.of(1L),
                GatewayRequestId.of("request-1"),
                UserAccountId.of(1L),
                ApiCredentialId.of(1L),
                ModelName.of("claude-sonnet"),
                ModelName.of("gpt-4.1"),
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.OPENAI_RESPONSES,
                ProviderChannelId.of(1L),
                UsageRecordStatus.SUCCESS,
                UsageTokenBreakdown.known(1L, 2L, 3L, 4L),
                false,
                STARTED_AT,
                ENDED_AT,
                UsageDuration.between(STARTED_AT, ENDED_AT),
                null,
                CREATED_AT
        );

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
