package com.api2api.ohs.http.dashboard.converter;

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
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DashboardResponseConverterTest {

    private static final Instant STARTED_AT = Instant.parse("2026-07-13T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-07-13T10:00:01Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-13T10:00:02Z");

    private final DashboardResponseConverter converter = Mappers.getMapper(DashboardResponseConverter.class);

    @Test
    void test_mapsUsageRecordFields_when_renderingRecentCall() {
        UsageRecord record = UsageRecord.rehydrate(
                UsageRecordId.of(8L),
                GatewayRequestId.of("request-8"),
                UserAccountId.of(2L),
                ApiCredentialId.of(3L),
                ModelName.of("claude-sonnet"),
                ModelName.of("gpt-4.1"),
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.OPENAI_RESPONSES,
                ProviderChannelId.of(7L),
                UsageRecordStatus.SUCCESS,
                UsageTokenBreakdown.known(100L, 20L, 30L, 40L),
                false,
                STARTED_AT,
                ENDED_AT,
                UsageDuration.between(STARTED_AT, ENDED_AT),
                null,
                CREATED_AT
        );

        var response = converter.toRecentCallResponse(record);

        assertThat(response)
                .extracting(
                        "id",
                        "apiCredentialId",
                        "requestedModel",
                        "requestProtocol",
                        "inputTokens",
                        "outputTokens",
                        "cacheCreationInputTokens",
                        "cacheReadInputTokens",
                        "totalTokens",
                        "usageKnown",
                        "createdAt"
                )
                .containsExactly(
                        8L,
                        3L,
                        "claude-sonnet",
                        ProtocolType.CLAUDE_MESSAGES,
                        100L,
                        20L,
                        30L,
                        40L,
                        190L,
                        true,
                        CREATED_AT
                );
    }
}
