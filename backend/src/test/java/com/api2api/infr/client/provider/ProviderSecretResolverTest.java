package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProviderKeyRef;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProviderSecretResolverTest {

    @Test
    void test_returnsPlaintextKey_when_referenceIsNotConfigured() {
        // Arrange
        ProviderSecretResolver resolver = new ProviderSecretResolver(
                new ProviderSecretProperties(),
                new MockEnvironment()
        );
        ProviderKeyRef plaintextKey = ProviderKeyRef.of("85b6e3f0-759b-422d-92b7-9655e8e049a2");

        // Act
        String resolvedKey = resolver.resolve(plaintextKey);

        // Assert
        assertThat(resolvedKey).isEqualTo(plaintextKey.value());
    }

    @Test
    void test_returnsConfiguredSecret_when_referenceIsConfigured() {
        // Arrange
        ProviderSecretProperties properties = new ProviderSecretProperties();
        properties.setKeys(Map.of("ANTHROPIC_API_KEY", "configured-secret"));
        ProviderSecretResolver resolver = new ProviderSecretResolver(properties, new MockEnvironment());

        // Act
        String resolvedKey = resolver.resolve(ProviderKeyRef.of("ANTHROPIC_API_KEY"));

        // Assert
        assertThat(resolvedKey).isEqualTo("configured-secret");
    }
}
