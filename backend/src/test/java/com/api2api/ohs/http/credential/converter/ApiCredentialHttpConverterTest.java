package com.api2api.ohs.http.credential.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ApiKeyPreview;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.credential.model.TokenLimit;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.credential.dto.ApiCredentialResponse;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiCredentialHttpConverterTest {

    private final ApiCredentialHttpConverter converter = new ApiCredentialHttpConverterImpl();

    @Test
    void test_initializes_usage_metrics_to_zero_when_mapping_new_credential() {
        // Arrange
        ApiCredential credential = ApiCredential.create(
                ApiCredentialId.of(1L),
                UserAccountId.of(2L),
                ApiCredentialName.of("production"),
                ApiKeyHash.of("a".repeat(64)),
                ApiKeyPreview.of("sk-***"),
                EncryptedApiKeyMaterial.unavailable(),
                ModelWhitelist.empty(),
                TokenLimit.unlimited(),
                Instant.parse("2026-07-14T00:00:00Z")
        );

        // Act
        ApiCredentialResponse response = converter.toResponse(credential);

        // Assert
        assertThat(response)
                .extracting(
                        ApiCredentialResponse::getConsumedTokens,
                        ApiCredentialResponse::getTodayConsumedTokens
                )
                .containsExactly(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
