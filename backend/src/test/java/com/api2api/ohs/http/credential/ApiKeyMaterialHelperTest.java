package com.api2api.ohs.http.credential;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyMaterialHelperTest {

    private final ApiKeyMaterialHelper helper = new ApiKeyMaterialHelper();

    @Test
    void test_generates_sk_prefixed_uuid_when_creating_api_key_material() {
        // Arrange
        String expectedPattern = "^sk-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

        // Act
        String plaintextKey = helper.generateApiKeyMaterial().getPlaintextKey();

        // Assert
        assertThat(plaintextKey).matches(expectedPattern);
    }

    @Test
    void test_generates_unique_keys_when_creating_multiple_api_key_materials() {
        // Arrange
        String firstKey = helper.generateApiKeyMaterial().getPlaintextKey();

        // Act
        String secondKey = helper.generateApiKeyMaterial().getPlaintextKey();

        // Assert
        assertThat(secondKey).isNotEqualTo(firstKey);
    }
}
