package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GatewayProtocolControllerTest {

    @Test
    void test_returns_only_whitelisted_models_when_api_key_is_valid() {
        // Arrange
        ApiCredentialApplicationService credentialService = mock(ApiCredentialApplicationService.class);
        GatewayApiKeyHashHelper keyHashHelper = mock(GatewayApiKeyHashHelper.class);
        ApiCredential credential = mock(ApiCredential.class);
        ApiKeyHash keyHash = ApiKeyHash.of("b".repeat(64));
        when(keyHashHelper.hashGatewayApiKey("Bearer requested-key", null)).thenReturn(keyHash);
        when(credentialService.authenticateForModelListing(keyHash)).thenReturn(credential);
        when(credential.getCreatedAt()).thenReturn(Instant.parse("2026-07-13T12:00:00Z"));
        when(credential.getModelWhitelist()).thenReturn(ModelWhitelist.of(Set.of(
                ModelName.of("gpt-4.1"),
                ModelName.of("claude-sonnet-4")
        )));
        GatewayProtocolController controller = new GatewayProtocolController(
                credentialService,
                keyHashHelper,
                mock(GatewayInvocationApplicationService.class),
                mock(GatewayRequestMapper.class),
                mock(GatewayInvocationResponseMapper.class),
                mock(GatewayStreamingResponseMapper.class),
                new ProtocolContractRegistry(new ObjectMapper())
        );

        // Act
        GatewayModelListResponse response = controller.listModels("Bearer requested-key", null);

        // Assert
        assertThat(response.object()).isEqualTo("list");
        assertThat(response.data())
                .extracting(GatewayModelResponse::id)
                .containsExactly("claude-sonnet-4", "gpt-4.1");
    }
}
