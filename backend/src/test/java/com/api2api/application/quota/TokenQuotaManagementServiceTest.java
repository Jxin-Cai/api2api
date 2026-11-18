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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TokenQuotaManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");
    private final TokenQuotaRepository repository = mock(TokenQuotaRepository.class);
    private final ApiCredentialRepository credentials = mock(ApiCredentialRepository.class);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final UserAccountId owner = UserAccountId.of(1L);
    private final TokenQuotaManagementService service = new TokenQuotaManagementService(repository, users,
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
        assertThat(result.rules()).extracting(TokenQuotaManagementService.RuleView::id).containsExactly("1", "2");
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

    @Test
    void test_saves_independent_user_rule_when_key_owner_sets_daily_limit() {
        // Arrange
        arrangeOwner();
        var rule = userRule(0, 10, "model-a");
        when(repository.save(rule)).thenReturn(userRule(30, 10, "model-a"));
        when(repository.consumed(any(), any())).thenReturn(BigDecimal.ZERO);
        // Act
        var saved = service.saveForCredential(owner, ApiCredentialId.of(10L), rule);
        // Assert
        assertThat(saved.source()).isEqualTo(QuotaSource.USER);
        verify(repository).save(rule);
    }

    @Test
    void test_denies_user_save_when_key_belongs_to_another_user() {
        // Arrange
        arrangeOwner();
        var other = UserAccountId.of(2L);
        when(users.findById(other)).thenReturn(Optional.of(mock(UserAccount.class)));
        // Act / Assert
        assertThatThrownBy(() -> service.saveForCredential(other, ApiCredentialId.of(10L), userRule(0, 10, "model-a")))
                .hasMessageStartingWith("ACCESS_DENIED:");
        verify(repository, never()).save(any());
    }

    @Test
    void test_denies_user_delete_when_key_belongs_to_another_user() {
        // Arrange
        arrangeOwner();
        var other = UserAccountId.of(2L);
        when(users.findById(other)).thenReturn(Optional.of(mock(UserAccount.class)));
        // Act / Assert
        assertThatThrownBy(() -> service.deleteForCredential(other, ApiCredentialId.of(10L), 30L))
                .hasMessageStartingWith("ACCESS_DENIED:");
        verify(repository, never()).deleteUserRule(anyLong(), anyLong());
    }

    @Test
    void test_rejects_admin_source_when_user_attempts_to_save_rule() {
        // Arrange
        var adminRule = new TokenQuotaRule(30, QuotaScope.API_KEY, 10, "model-a", QuotaPeriod.DAY, BigDecimal.ONE, true);
        // Act / Assert
        assertThatThrownBy(() -> service.saveForCredential(owner, ApiCredentialId.of(10L), adminRule))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void test_rejects_mismatched_target_when_user_submits_another_key_rule() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> service.saveForCredential(owner, ApiCredentialId.of(10L), userRule(30, 11, "model-a")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void test_rejects_enabled_rule_when_model_is_not_allowed_by_key() {
        // Arrange
        arrangeOwner();
        // Act / Assert
        assertThatThrownBy(() -> service.saveForCredential(owner, ApiCredentialId.of(10L), userRule(0, 10, "unapproved")))
                .hasMessageContaining("MODEL_NOT_ALLOWED");
        verify(repository, never()).save(any());
    }

    @Test
    void test_deletes_only_user_source_when_owner_removes_limit() {
        // Arrange
        arrangeOwner();
        when(repository.deleteUserRule(30, 10)).thenReturn(true);
        // Act
        service.deleteForCredential(owner, ApiCredentialId.of(10L), 30);
        // Assert
        verify(repository).deleteUserRule(30, 10);
        verify(repository, never()).delete(anyLong());
    }

    @Test
    void test_reports_missing_rule_when_owner_guesses_admin_rule_id() {
        // Arrange
        arrangeOwner();
        when(repository.deleteUserRule(99, 10)).thenReturn(false);
        // Act / Assert
        assertThatThrownBy(() -> service.deleteForCredential(owner, ApiCredentialId.of(10L), 99))
                .isInstanceOf(com.api2api.application.BusinessException.class);
    }

    @Test
    void test_lists_only_group_rules_when_admin_opens_quota_settings() {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        var group = groupRule(20);
        var legacyKey = new TokenQuotaRule(21, QuotaScope.API_KEY, 10, "model-a", QuotaPeriod.DAY, BigDecimal.ONE, true);
        var userKey = userRule(22, 10, "model-a");
        when(repository.findAll()).thenReturn(List.of(legacyKey, userKey, group));
        when(repository.consumed(eq(group), any())).thenReturn(BigDecimal.ZERO);
        // Act
        var page = service.list(owner);
        // Assert
        assertThat(page.rules()).extracting(TokenQuotaManagementService.RuleView::id).containsExactly("20");
        verify(repository, never()).consumed(eq(legacyKey), any());
        verify(repository, never()).consumed(eq(userKey), any());
    }

    @Test
    void test_saves_group_rule_when_admin_configures_shared_quota() {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(repository.targetExists(QuotaScope.MODEL_GROUP, 20)).thenReturn(true);
        when(repository.save(groupRule(0))).thenReturn(groupRule(30));
        when(repository.consumed(any(), any())).thenReturn(BigDecimal.ZERO);
        // Act
        var saved = service.save(owner, groupRule(0));
        // Assert
        assertThat(saved.scope()).isEqualTo(QuotaScope.MODEL_GROUP);
        verify(repository).save(groupRule(0));
    }

    @ParameterizedTest
    @EnumSource(QuotaSource.class)
    void test_rejects_key_quota_when_admin_calls_save(QuotaSource source) {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        var keyRule = new TokenQuotaRule(0, QuotaScope.API_KEY, 10, "model-a", QuotaPeriod.DAY, BigDecimal.ONE, true, source);
        // Act / Assert
        assertThatThrownBy(() -> service.save(owner, keyRule)).hasMessageContaining("Key 每日上限由用户在前台设置");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @EnumSource(QuotaSource.class)
    void test_rejects_update_when_admin_submits_key_rule_id_as_group(QuotaSource source) {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(repository.findById(30)).thenReturn(Optional.of(
                new TokenQuotaRule(30, QuotaScope.API_KEY, 10, "model-a", QuotaPeriod.DAY, BigDecimal.ONE, true, source)));
        // Act / Assert
        assertThatThrownBy(() -> service.save(owner, groupRule(30))).hasMessageContaining("模型分组限流规则不存在");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(QuotaSource.class)
    void test_rejects_delete_when_admin_submits_key_rule_id(QuotaSource source) {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(repository.findById(30)).thenReturn(Optional.of(
                new TokenQuotaRule(30, QuotaScope.API_KEY, 10, "model-a", QuotaPeriod.DAY, BigDecimal.ONE, true, source)));
        // Act / Assert
        assertThatThrownBy(() -> service.delete(owner, 30)).hasMessageContaining("模型分组限流规则不存在");
        verify(repository, never()).delete(anyLong());
    }

    @Test
    void test_updates_group_rule_when_admin_edits_existing_quota() {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(repository.findById(30)).thenReturn(Optional.of(groupRule(30)));
        when(repository.targetExists(QuotaScope.MODEL_GROUP, 20)).thenReturn(true);
        var updated = new TokenQuotaRule(30, QuotaScope.MODEL_GROUP, 20, "model-a", QuotaPeriod.DAY, BigDecimal.ONE, true);
        when(repository.save(updated)).thenReturn(updated);
        when(repository.consumed(any(), any())).thenReturn(BigDecimal.ZERO);
        // Act
        service.save(owner, updated);
        // Assert
        verify(repository).save(updated);
    }

    @Test
    void test_deletes_group_rule_when_admin_removes_shared_quota() {
        // Arrange
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(repository.findById(30)).thenReturn(Optional.of(groupRule(30)));
        when(repository.delete(30)).thenReturn(true);
        // Act
        service.delete(owner, 30);
        // Assert
        verify(repository).delete(30);
    }

    private TokenQuotaRule groupRule(long id) {
        return new TokenQuotaRule(id, QuotaScope.MODEL_GROUP, 20, "", QuotaPeriod.DAY, BigDecimal.TEN, true);
    }

    private void arrangeOwner() {
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(credentials.findById(ApiCredentialId.of(10L))).thenReturn(Optional.of(credential()));
    }

    private TokenQuotaRule userRule(long id, long keyId, String model) {
        return new TokenQuotaRule(id, QuotaScope.API_KEY, keyId, model, QuotaPeriod.DAY, BigDecimal.TEN, true, QuotaSource.USER);
    }

    private ApiCredential credential() {
        return ApiCredential.create(ApiCredentialId.of(10L), owner, ApiCredentialName.of("my-key"),
                ApiKeyHash.of("a".repeat(64)), ApiKeyPreview.of("test-preview"), EncryptedApiKeyMaterial.unavailable(),
                ModelGroupId.of(20L), ModelWhitelist.of(java.util.Set.of(ModelName.of("model-a"))), TokenLimit.unlimited(), NOW);
    }
}
