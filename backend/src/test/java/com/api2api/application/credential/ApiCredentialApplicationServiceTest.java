package com.api2api.application.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.api2api.application.credential.dto.ApiCredentialUsageView;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.usage.model.UsageTimeRange;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApiCredentialApplicationServiceTest {

    @Test
    void test_returns_accumulated_and_today_tokens_when_listing_credentials() {
        // Arrange
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        ApiCredentialRepository apiCredentialRepository = mock(ApiCredentialRepository.class);
        UsageRecordRepository usageRecordRepository = mock(UsageRecordRepository.class);
        ApiKeyMaterialProtector apiKeyMaterialProtector = mock(ApiKeyMaterialProtector.class);
        UserAccount userAccount = mock(UserAccount.class);
        ApiCredential credential = mock(ApiCredential.class);
        UserAccountId userAccountId = UserAccountId.of(1L);
        ApiCredentialId credentialId = ApiCredentialId.of(2L);
        UsageTimeRange todayTimeRange = UsageTimeRange.of(
                Instant.parse("2026-07-13T00:00:00Z"),
                Instant.parse("2026-07-14T00:00:00Z")
        );
        when(userAccountRepository.findById(userAccountId)).thenReturn(Optional.of(userAccount));
        when(apiCredentialRepository.findByOwnerUserId(userAccountId)).thenReturn(List.of(credential));
        when(credential.getId()).thenReturn(credentialId);
        when(usageRecordRepository.sumTotalTokensByApiCredential(credentialId)).thenReturn(120L);
        when(usageRecordRepository.sumTokens(any())).thenReturn(UsageTokenBreakdown.known(7, 3, 0, 0));
        ApiCredentialApplicationService service = new ApiCredentialApplicationService(
                userAccountRepository,
                apiCredentialRepository,
                usageRecordRepository,
                apiKeyMaterialProtector,
                Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC)
        );

        // Act
        List<ApiCredentialUsageView> views = service.listMyCredentialUsageViews(userAccountId, todayTimeRange);

        // Assert
        assertThat(views)
                .extracting(ApiCredentialUsageView::consumedTokens, ApiCredentialUsageView::todayConsumedTokens)
                .containsExactly(tuple(120L, 10L));
    }
}
