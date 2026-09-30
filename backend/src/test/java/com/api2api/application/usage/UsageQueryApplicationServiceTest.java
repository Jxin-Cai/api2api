package com.api2api.application.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.api2api.application.usage.command.QueryMyUsageRecordsCommand;
import com.api2api.application.usage.dto.PagedUsageRecordViews;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageDuration;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.usage.model.UsageRecordStatus;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserAccountStatus;
import com.api2api.domain.user.model.UserRole;
import com.api2api.domain.user.model.Username;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UsageQueryApplicationServiceTest {

    private static final Instant STARTED_AT = Instant.parse("2026-09-10T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-09-10T10:00:01Z");

    @Test
    void test_exposes_api_credential_name_when_querying_my_usage_records() {
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        ApiCredentialRepository apiCredentialRepository = mock(ApiCredentialRepository.class);
        ProviderChannelRepository providerChannelRepository = mock(ProviderChannelRepository.class);
        UsageRecordRepository usageRecordRepository = mock(UsageRecordRepository.class);
        UsageQueryApplicationService service = new UsageQueryApplicationService(
                userAccountRepository,
                apiCredentialRepository,
                providerChannelRepository,
                usageRecordRepository
        );
        UserAccountId userId = UserAccountId.of(1L);
        ApiCredentialId credentialId = ApiCredentialId.of(9L);
        UserAccount user = UserAccount.rehydrate(
                userId,
                Username.of("alice"),
                DisplayName.of("Alice"),
                UserRole.USER,
                UserAccountStatus.ACTIVE,
                null,
                STARTED_AT,
                STARTED_AT
        );
        ApiCredential credential = mock(ApiCredential.class);
        UsageRecord record = UsageRecord.rehydrate()
                .id(UsageRecordId.of(3L))
                .requestId(GatewayRequestId.of("request-3"))
                .userAccountId(userId)
                .apiCredentialId(credentialId)
                .requestedModel(ModelName.of("claude-sonnet"))
                .clientIp("127.0.0.1")
                .firstTokenMillis(12L)
                .upstreamModel(ModelName.of("claude-sonnet"))
                .requestProtocol(ProtocolType.CLAUDE_MESSAGES)
                .upstreamProtocol(ProtocolType.CLAUDE_MESSAGES)
                .providerChannelId(ProviderChannelId.of(7L))
                .status(UsageRecordStatus.SUCCESS)
                .tokenUsage(UsageTokenBreakdown.known(10L, 20L, 0L, 0L))
                .streaming(false)
                .startedAt(STARTED_AT)
                .endedAt(ENDED_AT)
                .duration(UsageDuration.between(STARTED_AT, ENDED_AT))
                .errorDiagnostic(null)
                .createdAt(ENDED_AT)
                .build();
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(credential.getName()).thenReturn(ApiCredentialName.of("生产 Key"));
        when(apiCredentialRepository.findById(credentialId)).thenReturn(Optional.of(credential));
        when(usageRecordRepository.query(any(), any())).thenReturn(
                PagedUsageRecords.of(List.of(record), 1, 50, 1L, UsageTokenBreakdown.known(10L, 20L, 0L, 0L))
        );

        PagedUsageRecordViews page = service.queryMyUsageRecords(QueryMyUsageRecordsCommand.builder()
                .currentUserId(userId)
                .startInclusive(STARTED_AT)
                .endExclusive(ENDED_AT)
                .page(1)
                .size(50)
                .build());

        assertThat(page.getRecords().get(0).getApiCredentialName()).isEqualTo("生产 Key");
    }
}
