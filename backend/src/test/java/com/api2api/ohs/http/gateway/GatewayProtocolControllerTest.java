package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.api2api.application.BusinessException;
import com.api2api.application.channel.ProviderModelAvailabilityService;
import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.MultipartFormPayloadCodec;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GatewayProtocolControllerTest {

    private final ApiCredentialApplicationService credentialService = mock(ApiCredentialApplicationService.class);
    private final GatewayApiKeyHashHelper keyHashHelper = mock(GatewayApiKeyHashHelper.class);
    private final ApiCredential credential = mock(ApiCredential.class);
    private final ApiKeyHash keyHash = ApiKeyHash.of("b".repeat(64));
    private final ProviderModelAvailabilityService availability = mock(ProviderModelAvailabilityService.class);
    private final GatewayProtocolController controller = new GatewayProtocolController(
            credentialService, keyHashHelper, mock(GatewayInvocationApplicationService.class),
            mock(GatewayRequestMapper.class), mock(GatewayInvocationResponseMapper.class),
            mock(GatewayStreamingResponseMapper.class), new ProtocolContractRegistry(new ObjectMapper()),
            mock(MultipartFormRequestReader.class), mock(MultipartFormPayloadCodec.class),
            mock(ResponsesImageBridge.class), availability);

    @BeforeEach
    void setUp() {
        when(keyHashHelper.hashGatewayApiKey("Bearer requested-key", null)).thenReturn(keyHash);
        when(credentialService.authenticateForModelListing(keyHash)).thenReturn(credential);
        when(credential.getCreatedAt()).thenReturn(Instant.parse("2026-07-13T12:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v1/model", "/v1/models"})
    void test_returns_only_supported_whitelisted_models_when_api_key_is_valid(String path) throws Exception {
        // Arrange
        when(credential.getModelWhitelist()).thenReturn(ModelWhitelist.of(Set.of(
                ModelName.of("gpt-4.1"),
                ModelName.of("claude-sonnet-4"),
                ModelName.of("unsupported-model")
        )));
        when(availability.availableModels()).thenReturn(Set.of(
                ModelName.of("gpt-4.1"), ModelName.of("claude-sonnet-4"), ModelName.of("not-allowed")));

        // Act
        var result = MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get(path).header("Authorization", "Bearer requested-key"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("list"));
        String body = result.andReturn().getResponse().getContentAsString();
        assertThat(new ObjectMapper().readTree(body).path("data").findValuesAsText("id"))
                .as("Returned model list: %s", body)
                .containsExactly("claude-sonnet-4", "gpt-4.1");
    }

    @Test
    void test_returns_empty_list_when_no_provider_supports_whitelisted_models() {
        // Arrange
        when(credential.getModelWhitelist()).thenReturn(ModelWhitelist.of(Set.of(ModelName.of("configured"))));
        when(availability.availableModels()).thenReturn(Set.of());

        // Act
        GatewayModelListResponse response = controller.listModels("Bearer requested-key", null);

        // Assert
        assertThat(response.data()).isEmpty();
    }

    @Test
    void test_returns_empty_list_when_group_forbids_all_models() {
        // Arrange
        when(credential.getModelWhitelist()).thenReturn(ModelWhitelist.empty());
        when(availability.availableModels()).thenReturn(Set.of(ModelName.of("supported")));

        // Act
        GatewayModelListResponse response = controller.listModels("Bearer requested-key", null);

        // Assert
        assertThat(response.data()).isEmpty();
    }

    @Test
    void test_refreshes_model_list_when_provider_support_changes() {
        // Arrange
        ModelName model = ModelName.of("configured");
        when(credential.getModelWhitelist()).thenReturn(ModelWhitelist.of(Set.of(model)));
        when(availability.availableModels()).thenReturn(Set.of(model)).thenReturn(Set.of()).thenReturn(Set.of(model));

        // Act
        var before = controller.listModels("Bearer requested-key", null).data();
        var disabled = controller.listModels("Bearer requested-key", null).data();
        var restored = controller.listModels("Bearer requested-key", null).data();

        // Assert
        assertThat(List.of(before.size(), disabled.size(), restored.size())).containsExactly(1, 0, 1);
    }

    @Test
    void test_rejects_model_listing_when_credential_authentication_fails() {
        // Arrange
        when(credentialService.authenticateForModelListing(keyHash)).thenThrow(new BusinessException("API_CREDENTIAL_INVALID"));

        // Act / Assert
        assertThatThrownBy(() -> controller.listModels("Bearer requested-key", null))
                .isInstanceOf(BusinessException.class).hasMessage("API_CREDENTIAL_INVALID");
        verifyNoInteractions(availability);
    }
}
