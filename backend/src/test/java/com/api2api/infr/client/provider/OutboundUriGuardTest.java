package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class OutboundUriGuardTest {

    @Test
    void test_returnsUri_when_httpUpstreamUsesPublicHost() {
        // Arrange
        URI uri = URI.create("http://8.8.8.8/v1/responses");

        // Act
        URI verifiedUri = OutboundUriGuard.verify(uri, false);

        // Assert
        assertThat(verifiedUri).isEqualTo(uri);
    }

    @Test
    void test_rejectsLocalhost_when_insecureHostsAreDisabled() {
        // Arrange
        URI uri = URI.create("http://localhost/v1/responses");

        // Act & Assert
        assertThatThrownBy(() -> OutboundUriGuard.verify(uri, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upstream host is not allowed");
    }

    @Test
    void test_rejectsUnsupportedScheme_when_upstreamUsesFtp() {
        // Arrange
        URI uri = URI.create("ftp://8.8.8.8/v1/responses");

        // Act & Assert
        assertThatThrownBy(() -> OutboundUriGuard.verify(uri, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upstream URI must use http or https");
    }
}
