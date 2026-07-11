package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.credential.model.ApiKeyHash;
import org.junit.jupiter.api.Test;

class GatewayApiKeyHashHelperTest {

    private final GatewayApiKeyHashHelper helper = new GatewayApiKeyHashHelper();

    @Test
    void shouldHashApiKeyHeaderWhenAuthorizationIsAbsent() {
        ApiKeyHash hash = helper.hashGatewayApiKey(null, " api2api_test_key ");

        assertThat(hash.value()).hasSize(64);
        assertThat(hash.value()).isEqualTo(helper.hashBearerToken("Bearer api2api_test_key").value());
    }

    @Test
    void shouldPreferBearerTokenWhenBothHeadersArePresent() {
        ApiKeyHash hash = helper.hashGatewayApiKey("Bearer bearer_key", "api_key_header");

        assertThat(hash.value()).isEqualTo(helper.hashBearerToken("Bearer bearer_key").value());
    }

    @Test
    void shouldRejectRequestsWithoutSupportedApiKeyHeader() {
        assertThatThrownBy(() -> helper.hashGatewayApiKey(null, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authorization bearer token or x-api-key is required");
    }
}
