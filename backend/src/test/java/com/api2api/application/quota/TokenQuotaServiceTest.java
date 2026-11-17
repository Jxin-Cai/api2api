package com.api2api.application.quota;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.api2api.domain.credential.model.*;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.quota.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TokenQuotaServiceTest {
    private final TokenQuotaRepository repository = mock(TokenQuotaRepository.class);
    private final ApiCredential credential = mock(ApiCredential.class);
    private final TokenQuotaService service = new TokenQuotaService(repository, mock(ApiCredentialRepository.class),
            new TokenQuotaWindow(Clock.fixed(Instant.parse("2026-10-26T17:00:00Z"), ZoneOffset.UTC), "Asia/Shanghai"));
    private final ModelName model = ModelName.of("gpt-6-astra");

    @BeforeEach
    void arrangeCredential() {
        when(credential.getId()).thenReturn(ApiCredentialId.of(10L));
        when(credential.getModelGroupId()).thenReturn(ModelGroupId.of(20L));
    }

    @ParameterizedTest
    @EnumSource(QuotaPeriod.class)
    void test_rejects_request_when_group_period_budget_is_exhausted(QuotaPeriod period) {
        // Arrange
        var rule = new TokenQuotaRule(1, QuotaScope.MODEL_GROUP, 20, "", period, BigDecimal.TEN, true);
        when(repository.findApplicable(20, 10)).thenReturn(List.of(rule));
        when(repository.consumed(eq(rule), any())).thenReturn(BigDecimal.TEN);
        // Act / Assert
        assertThatThrownBy(() -> service.assertAvailable(credential, model)).isInstanceOf(TokenQuotaExceededException.class);
    }

    @Test
    void test_rejects_key_when_group_budget_still_has_capacity() {
        // Arrange
        var group = new TokenQuotaRule(1, QuotaScope.MODEL_GROUP, 20, "", QuotaPeriod.DAY, BigDecimal.TEN, true);
        var key = new TokenQuotaRule(2, QuotaScope.API_KEY, 10, "", QuotaPeriod.MONTH, BigDecimal.ONE, true);
        when(repository.findApplicable(20, 10)).thenReturn(List.of(group, key));
        when(repository.consumed(eq(group), any())).thenReturn(BigDecimal.ZERO);
        when(repository.consumed(eq(key), any())).thenReturn(BigDecimal.ONE);
        // Act / Assert
        assertThatThrownBy(() -> service.assertAvailable(credential, model)).hasMessageContaining("API_KEY MONTH");
    }

    @Test
    void test_rejects_group_when_key_budget_still_has_capacity() {
        // Arrange
        var key = new TokenQuotaRule(1, QuotaScope.API_KEY, 10, "", QuotaPeriod.DAY, BigDecimal.TEN, true);
        var group = new TokenQuotaRule(2, QuotaScope.MODEL_GROUP, 20, "", QuotaPeriod.WEEK, BigDecimal.ONE, true);
        when(repository.findApplicable(20, 10)).thenReturn(List.of(key, group));
        when(repository.consumed(eq(key), any())).thenReturn(BigDecimal.ZERO);
        when(repository.consumed(eq(group), any())).thenReturn(BigDecimal.ONE);
        // Act / Assert
        assertThatThrownBy(() -> service.assertAvailable(credential, model)).hasMessageContaining("MODEL_GROUP WEEK");
    }

    @Test
    void test_allows_other_model_when_rule_limits_specific_model() {
        // Arrange
        var rule = new TokenQuotaRule(1, QuotaScope.MODEL_GROUP, 20, "another-model", QuotaPeriod.DAY, BigDecimal.ONE, true);
        when(repository.findApplicable(20, 10)).thenReturn(List.of(rule));
        // Act
        service.assertAvailable(credential, model);
        // Assert
        verify(repository, never()).consumed(any(), any());
    }

    @Test
    void test_limits_models_independently_when_one_key_model_exhausts_daily_budget() {
        // Arrange
        var exhausted = new TokenQuotaRule(1, QuotaScope.API_KEY, 10, model.value(), QuotaPeriod.DAY, BigDecimal.TEN, true);
        var other = new TokenQuotaRule(2, QuotaScope.API_KEY, 10, "other-model", QuotaPeriod.DAY, BigDecimal.valueOf(50), true);
        when(repository.findApplicable(20, 10)).thenReturn(List.of(exhausted, other));
        when(repository.consumed(eq(exhausted), any())).thenReturn(BigDecimal.TEN);
        when(repository.consumed(eq(other), any())).thenReturn(BigDecimal.ONE);
        // Act / Assert
        assertThatThrownBy(() -> service.assertAvailable(credential, model)).isInstanceOf(TokenQuotaExceededException.class);
        assertThatCode(() -> service.assertAvailable(credential, ModelName.of("other-model"))).doesNotThrowAnyException();
    }

    @Test
    void test_keeps_other_key_available_when_first_key_exhausts_same_model() {
        // Arrange
        var first = new TokenQuotaRule(1, QuotaScope.API_KEY, 10, model.value(), QuotaPeriod.DAY, BigDecimal.ONE, true);
        var second = new TokenQuotaRule(2, QuotaScope.API_KEY, 11, model.value(), QuotaPeriod.DAY, BigDecimal.TEN, true);
        var anotherCredential = mock(ApiCredential.class);
        when(anotherCredential.getId()).thenReturn(ApiCredentialId.of(11L));
        when(anotherCredential.getModelGroupId()).thenReturn(ModelGroupId.of(20L));
        when(repository.findApplicable(20, 10)).thenReturn(List.of(first));
        when(repository.findApplicable(20, 11)).thenReturn(List.of(second));
        when(repository.consumed(eq(first), any())).thenReturn(BigDecimal.ONE);
        when(repository.consumed(eq(second), any())).thenReturn(BigDecimal.ZERO);
        // Act / Assert
        assertThatThrownBy(() -> service.assertAvailable(credential, model)).isInstanceOf(TokenQuotaExceededException.class);
        assertThatCode(() -> service.assertAvailable(anotherCredential, model)).doesNotThrowAnyException();
    }

    @Test
    void test_resets_key_model_budget_when_calendar_day_changes() {
        // Arrange
        var rule = new TokenQuotaRule(1, QuotaScope.API_KEY, 10, model.value(), QuotaPeriod.DAY, BigDecimal.ONE, true);
        when(repository.findApplicable(20, 10)).thenReturn(List.of(rule));
        var midnight = Instant.parse("2026-10-27T16:00:00Z");
        when(repository.consumed(eq(rule), any())).thenAnswer(call -> {
            com.api2api.domain.usage.model.UsageTimeRange window = call.getArgument(1);
            return window.startInclusive().isBefore(midnight) ? BigDecimal.ONE : BigDecimal.ZERO;
        });
        var nextDay = new TokenQuotaService(repository, mock(ApiCredentialRepository.class),
                new TokenQuotaWindow(Clock.fixed(midnight, ZoneOffset.UTC), "Asia/Shanghai"));
        // Act / Assert
        assertThatThrownBy(() -> service.assertAvailable(credential, model)).isInstanceOf(TokenQuotaExceededException.class);
        assertThatCode(() -> nextDay.assertAvailable(credential, model)).doesNotThrowAnyException();
    }

    @Test
    void test_allows_request_when_consumption_is_below_limit() {
        // Arrange
        var rule = new TokenQuotaRule(1, QuotaScope.MODEL_GROUP, 20, "", QuotaPeriod.DAY, BigDecimal.TEN, true);
        when(repository.findApplicable(20, 10)).thenReturn(List.of(rule));
        when(repository.consumed(eq(rule), any())).thenReturn(new BigDecimal("9.95"));
        // Act / Assert
        assertThatCode(() -> service.assertAvailable(credential, model)).doesNotThrowAnyException();
    }
}
