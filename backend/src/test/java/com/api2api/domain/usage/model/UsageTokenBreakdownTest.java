package com.api2api.domain.usage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UsageTokenBreakdownTest {

    @Test
    void test_calculates_weighted_actual_tokens_when_all_token_types_are_present() {
        // Arrange
        UsageTokenBreakdown breakdown = UsageTokenBreakdown.known(100, 20, 8, 30);

        // Act
        BigDecimal actualTokens = breakdown.actualTokens();

        // Assert
        assertThat(actualTokens).isEqualByComparingTo("213");
    }
}
