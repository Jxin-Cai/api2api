package com.api2api.domain.credential.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.user.model.UserAccountId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelGroupTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-20T00:00:00Z");
    private static final ModelName GPT = ModelName.of("gpt-4.1");
    private static final ModelName CLAUDE = ModelName.of("claude-sonnet-4");

    @Test
    void test_updates_whitelist_when_group_configuration_changes() {
        // Arrange
        ModelGroup group = groupWithLimits(Map.of());
        ModelWhitelist changedWhitelist = ModelWhitelist.of(Set.of(CLAUDE));

        // Act
        group.update(ModelGroupName.of("production"), changedWhitelist, ModelDailyLimits.empty(), CREATED_AT.plusSeconds(60));

        // Assert
        assertThat(group.getModelWhitelist()).isEqualTo(changedWhitelist);
    }

    @Test
    void test_rejects_request_when_model_daily_limit_is_reached() {
        // Arrange
        ModelGroup group = groupWithLimits(Map.of(GPT, 1_000L));

        // Act / Assert
        assertThatThrownBy(() -> group.assertDailyLimitAvailable(GPT, new BigDecimal("1000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("MODEL_DAILY_LIMIT_EXCEEDED");
    }

    @Test
    void test_allows_request_when_model_has_no_daily_limit() {
        // Arrange
        ModelGroup group = groupWithLimits(Map.of(GPT, 1_000L));

        // Act / Assert
        assertThatCode(() -> group.assertDailyLimitAvailable(CLAUDE, new BigDecimal("999999")))
                .doesNotThrowAnyException();
    }

    @Test
    void test_reports_rate_limited_models_when_consumption_reaches_limits() {
        // Arrange
        ModelGroup group = groupWithLimits(Map.of(GPT, 1_000L, CLAUDE, 500L));

        // Act
        Set<ModelName> limited = group.rateLimitedModels(Map.of(GPT, new BigDecimal("1200"), CLAUDE, new BigDecimal("10")));

        // Assert
        assertThat(limited).containsExactly(GPT);
    }

    private static ModelGroup groupWithLimits(Map<ModelName, Long> limits) {
        return ModelGroup.create(
                ModelGroupId.of(1L),
                UserAccountId.of(2L),
                ModelGroupName.of("production"),
                ModelWhitelist.of(Set.of(GPT, CLAUDE)),
                ModelDailyLimits.of(limits),
                CREATED_AT
        );
    }
}
