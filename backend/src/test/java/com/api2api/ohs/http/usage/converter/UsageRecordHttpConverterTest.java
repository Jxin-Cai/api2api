package com.api2api.ohs.http.usage.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.application.usage.dto.PagedUsageRecordViews;
import com.api2api.application.usage.dto.UsageRecordView;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageDuration;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.usage.model.UsageRecordStatus;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.user.model.UserAccountId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UsageRecordHttpConverterTest {

    private static final Instant STARTED_AT = Instant.parse("2026-07-08T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-07-08T10:00:01Z");

    private final UsageRecordHttpConverter converter = Mappers.getMapper(UsageRecordHttpConverter.class);

    @Test
    void test_hides_provider_channel_when_rendering_user_usage_records() {
        PagedUsageRecordViews page = pageWith(successfulRecord());

        var response = converter.toPageResponse(page, false);

        assertThat(response.getRecords().get(0).getProviderChannelId()).isNull();
        assertThat(response.getRecords().get(0).getProviderChannelName()).isNull();
    }

    @Test
    void test_includes_provider_channel_when_rendering_admin_usage_records() {
        PagedUsageRecordViews page = pageWith(successfulRecord());

        var response = converter.toPageResponse(page, true);

        assertThat(response.getRecords().get(0).getProviderChannelId()).isEqualTo(7L);
        assertThat(response.getRecords().get(0).getProviderChannelName()).isEqualTo("channel-a");
    }

    private PagedUsageRecordViews pageWith(UsageRecord record) {
        PagedUsageRecords page = PagedUsageRecords.of(
                List.of(record),
                1,
                50,
                1L,
                UsageTokenBreakdown.known(1L, 2L, 0L, 0L)
        );
        return PagedUsageRecordViews.of(
                List.of(UsageRecordView.of(record, "user-a", "credential-a", "channel-a")),
                page
        );
    }

    private UsageRecord successfulRecord() {
        return UsageRecord.rehydrate(
                UsageRecordId.of(1L),
                GatewayRequestId.of("request-1"),
                UserAccountId.of(1L),
                ApiCredentialId.of(1L),
                ModelName.of("claude-sonnet"),
                ModelName.of("gpt-4.1"),
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.OPENAI_RESPONSES,
                ProviderChannelId.of(7L),
                UsageRecordStatus.SUCCESS,
                UsageTokenBreakdown.known(1L, 2L, 0L, 0L),
                false,
                STARTED_AT,
                ENDED_AT,
                UsageDuration.between(STARTED_AT, ENDED_AT),
                null,
                ENDED_AT
        );
    }
}
