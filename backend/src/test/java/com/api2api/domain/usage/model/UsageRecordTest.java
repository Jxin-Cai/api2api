package com.api2api.domain.usage.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.user.model.UserAccountId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsageRecordTest {

    private static final Instant STARTED_AT = Instant.parse("2026-07-08T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-07-08T10:00:01.250Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-08T10:00:02Z");

    @Test
    void rehydrateAcceptsDurationMatchingEndedAtMinusStartedAt() {
        UsageRecord record = usageRecord(UsageDuration.between(STARTED_AT, ENDED_AT), ENDED_AT);

        assertThat(record.duration().millis()).isEqualTo(1250L);
    }

    @Test
    void rehydrateRejectsDurationThatDoesNotMatchEndedAtMinusStartedAt() {
        assertThatThrownBy(() -> usageRecord(UsageDuration.of(1L), ENDED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usage duration must equal endedAt minus startedAt");
    }

    @Test
    void rehydrateRejectsEndedAtBeforeStartedAt() {
        Instant endedBeforeStarted = STARTED_AT.minusMillis(1L);

        assertThatThrownBy(() -> usageRecord(UsageDuration.of(0L), endedBeforeStarted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ended time must not be before started time");
    }

    private UsageRecord usageRecord(UsageDuration duration, Instant endedAt) {
        return UsageRecord.rehydrate(
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
                endedAt,
                duration,
                null,
                CREATED_AT
        );
    }
}
