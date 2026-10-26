package com.api2api.domain.routing.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ScopedRateLimitTest {
    @ParameterizedTest
    @ValueSource(strings = {"gateway_concurrency_limit", "group_rate_limit_exceeded", "group_quota_exceeded", "user_rate_limit_exceeded", "api_key_rate_limit_exceeded", "TOKEN_RATE_LIMIT_EXCEEDED"})
    void test_classifies_caller_scope_when_upstream_returns_scoped_429(String code) {
        // Arrange
        String body = "{\"error\":{\"code\":\"" + code + "\",\"type\":\"rate_limit_error\"}}";
        // Act
        var type = RouteFailureType.fromHttpResponse(429, body);
        // Assert
        assertThat(type).isEqualTo(RouteFailureType.CALLER_RATE_LIMITED);
    }

    @Test
    void test_preserves_provider_isolation_when_limit_has_no_caller_scope() {
        // Arrange / Act
        var type = RouteFailureType.fromHttpResponse(429, "{\"error\":{\"code\":\"rate_limit_exceeded\"}}");
        // Assert
        assertThat(type).isEqualTo(RouteFailureType.RATE_LIMITED);
    }
}
