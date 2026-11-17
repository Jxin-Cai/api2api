package com.api2api.application.quota;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.api2api.domain.credential.model.*;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.quota.*;
import com.api2api.domain.user.model.*;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TokenQuotaAdminServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");
    private final TokenQuotaRepository repository = mock(TokenQuotaRepository.class);
    private final ApiCredentialRepository credentials = mock(ApiCredentialRepository.class);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final UserAccountId owner = UserAccountId.of(1L);
    private final TokenQuotaAdminService service = new TokenQuotaAdminService(repository, users,
            new TokenQuotaWindow(Clock.fixed(NOW, ZoneOffset.UTC), "Asia/Shanghai"), credentials);

    @Test
    void test_returns_only_own_key_and_group_quotas_when_owner_views_limits() {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        var credential = credential();
        when(credentials.findById(credential.getId())).thenReturn(Optional.of(credential));
        var keyRule = new TokenQuotaRule(1, QuotaScope.API_KEY, 10, "model-a", QuotaPeriod.DAY, BigDecimal.TEN, true);
        var groupRule = new TokenQuotaRule(2, QuotaScope.MODEL_GROUP, 20, "", QuotaPeriod.MONTH, BigDecimal.TEN, true);
        when(repository.findByTarget(QuotaScope.API_KEY, 10)).thenReturn(List.of(keyRule));
        when(repository.findByTarget(QuotaScope.MODEL_GROUP, 20)).thenReturn(List.of(groupRule));
        when(repository.consumed(any(), any())).thenReturn(BigDecimal.ONE);
        // Act
        var result = service.listForCredential(owner, credential.getId());
        // Assert
        assertThat(result.rules()).extracting(TokenQuotaAdminService.RuleView::id).containsExactly("1", "2");
        assertThat(result.targets()).isEmpty();
        verify(repository, never()).findAll();
        verify(repository, never()).targets();
    }

    @Test
    void test_denies_read_when_key_belongs_to_another_user() {
        // Arrange
        var other = UserAccountId.of(2L);
        when(users.findById(other)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(credentials.findById(ApiCredentialId.of(10L))).thenReturn(Optional.of(credential()));
        // Act / Assert
        assertThatThrownBy(() -> service.listForCredential(other, ApiCredentialId.of(10L)))
                .hasMessageStartingWith("ACCESS_DENIED:");
        verifyNoInteractions(repository);
    }

    @Test
    void test_denies_save_when_operator_is_not_admin() {
        // Arrange
        var user = UserAccount.create(owner, Username.of("normal-user"), DisplayName.of("Normal"),
                UserRole.USER, PasswordHash.of("test-password-hash"), NOW);
        when(users.findById(owner)).thenReturn(Optional.of(user));
        var rule = new TokenQuotaRule(0, QuotaScope.API_KEY, 10, "model-a", QuotaPeriod.DAY, BigDecimal.ONE, true);
        // Act / Assert
        assertThatThrownBy(() -> service.save(owner, rule)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(repository);
    }

    private ApiCredential credential() {
        return ApiCredential.create(ApiCredentialId.of(10L), owner, ApiCredentialName.of("my-key"),
                ApiKeyHash.of("a".repeat(64)), ApiKeyPreview.of("test-preview"), EncryptedApiKeyMaterial.unavailable(),
                ModelGroupId.of(20L), ModelWhitelist.empty(), TokenLimit.unlimited(), NOW);
    }
}
