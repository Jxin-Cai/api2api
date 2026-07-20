package com.api2api.application.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.application.credential.command.DeleteApiCredentialCommand;
import com.api2api.application.credential.dto.ApiCredentialUsageView;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import com.api2api.domain.usage.model.UsageTimeRange;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApiCredentialApplicationServiceTest {

    @Test
    void test_returns_credential_whitelist_when_api_key_is_active() {
        // Arrange
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        ApiCredentialRepository apiCredentialRepository = mock(ApiCredentialRepository.class);
        ModelGroupRepository modelGroupRepository = mock(ModelGroupRepository.class);
        UsageRecordRepository usageRecordRepository = mock(UsageRecordRepository.class);
        ApiKeyMaterialProtector apiKeyMaterialProtector = mock(ApiKeyMaterialProtector.class);
        ApiCredential credential = mock(ApiCredential.class);
        ApiKeyHash keyHash = ApiKeyHash.of("a".repeat(64));
        when(apiCredentialRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(credential));
        ApiCredentialApplicationService service = new ApiCredentialApplicationService(
                userAccountRepository,
                apiCredentialRepository,
                modelGroupRepository,
                usageRecordRepository,
                apiKeyMaterialProtector,
                Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC)
        );

        // Act
        ApiCredential result = service.authenticateForModelListing(keyHash);

        // Assert
        assertThat(result).isSameAs(credential);
        verify(credential).assertUsable();
    }

    @Test
    void test_soft_deletes_credential_when_owner_requests_deletion() {
        // Arrange
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        ApiCredentialRepository apiCredentialRepository = mock(ApiCredentialRepository.class);
        ModelGroupRepository modelGroupRepository = mock(ModelGroupRepository.class);
        UsageRecordRepository usageRecordRepository = mock(UsageRecordRepository.class);
        ApiKeyMaterialProtector apiKeyMaterialProtector = mock(ApiKeyMaterialProtector.class);
        UserAccount userAccount = mock(UserAccount.class);
        ApiCredential credential = mock(ApiCredential.class);
        UserAccountId userAccountId = UserAccountId.of(1L);
        ApiCredentialId credentialId = ApiCredentialId.of(2L);
        Instant now = Instant.parse("2026-07-13T12:00:00Z");
        when(userAccountRepository.findById(userAccountId)).thenReturn(Optional.of(userAccount));
        when(apiCredentialRepository.findById(credentialId)).thenReturn(Optional.of(credential));
        ApiCredentialApplicationService service = new ApiCredentialApplicationService(
                userAccountRepository,
                apiCredentialRepository,
                modelGroupRepository,
                usageRecordRepository,
                apiKeyMaterialProtector,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        DeleteApiCredentialCommand command = DeleteApiCredentialCommand.builder()
                .ownerUserId(userAccountId)
                .apiCredentialId(credentialId)
                .build();

        // Act
        service.deleteCredential(command);

        // Assert
        verify(apiCredentialRepository).softDeleteById(credentialId, now);
    }

    @Test
    void test_returns_actual_and_total_tokens_when_listing_credentials() {
        // Arrange
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        ApiCredentialRepository apiCredentialRepository = mock(ApiCredentialRepository.class);
        ModelGroupRepository modelGroupRepository = mock(ModelGroupRepository.class);
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
        when(usageRecordRepository.sumActualTokensByApiCredential(credentialId)).thenReturn(new BigDecimal("156.25"));
        when(usageRecordRepository.sumTokens(any())).thenReturn(UsageTokenBreakdown.known(7, 3, 2, 1));
        ApiCredentialApplicationService service = new ApiCredentialApplicationService(
                userAccountRepository,
                apiCredentialRepository,
                modelGroupRepository,
                usageRecordRepository,
                apiKeyMaterialProtector,
                Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC)
        );

        // Act
        List<ApiCredentialUsageView> views = service.listMyCredentialUsageViews(userAccountId, todayTimeRange);

        // Assert
        assertThat(views)
                .extracting(
                        ApiCredentialUsageView::consumedTokens,
                        ApiCredentialUsageView::totalTokens,
                        ApiCredentialUsageView::todayConsumedTokens,
                        ApiCredentialUsageView::todayTotalTokens
                )
                .containsExactly(tuple(new BigDecimal("156.25"), 120L, new BigDecimal("24.60"), 13L));
    }
}
