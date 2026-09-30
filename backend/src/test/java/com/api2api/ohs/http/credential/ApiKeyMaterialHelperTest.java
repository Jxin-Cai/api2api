package com.api2api.ohs.http.credential;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.ohs.http.gateway.GatewayApiKeyHashHelper;
import org.junit.jupiter.api.Test;

class ApiKeyMaterialHelperTest {

    private final ApiKeyMaterialHelper helper = new ApiKeyMaterialHelper();

    @Test
    void test_preservesStoredHashFormat_when_hashingKnownKey() {
        // Arrange
        String plaintextKey = "abc";

        // Act
        var hash = helper.hashKey(plaintextKey);

        // Assert
        assertThat(hash.value()).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void test_matchesIssuedKeyHash_when_gatewayAuthenticatesGeneratedKey() {
        // Arrange
        var material = helper.generateApiKeyMaterial();
        var gatewayHasher = new GatewayApiKeyHashHelper();

        // Act
        var gatewayHash = gatewayHasher.hashGatewayApiKey("Bearer " + material.getPlaintextKey(), null);

        // Assert
        assertThat(gatewayHash).isEqualTo(material.getKeyHash());
    }

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
