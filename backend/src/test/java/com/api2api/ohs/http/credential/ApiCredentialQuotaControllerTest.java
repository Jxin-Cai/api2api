package com.api2api.ohs.http.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.api2api.application.quota.TokenQuotaManagementService;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.quota.*;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.GlobalExceptionAdvice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiCredentialQuotaControllerTest {
    private final TokenQuotaManagementService service = mock(TokenQuotaManagementService.class);
    private final CurrentUserContextResolver users = mock(CurrentUserContextResolver.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ApiCredentialQuotaController(service, users))
            .setControllerAdvice(new GlobalExceptionAdvice()).build();

    @Test
    void test_uses_server_owned_scope_when_client_spoofs_rule_metadata() throws Exception {
        // Arrange
        when(users.resolveCurrentUserId(any())).thenReturn(UserAccountId.of(1L));
        String body = """
                {"model":"model-a","limitMillions":0.5,"enabled":true,
                 "source":"ADMIN","scope":"MODEL_GROUP","targetId":999,"period":"LIFETIME"}
                """;
        // Act
        mvc.perform(post("/api/api-credentials/10/token-quotas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        // Assert
        var rule = ArgumentCaptor.forClass(TokenQuotaRule.class);
        verify(service).saveForCredential(eq(UserAccountId.of(1L)), eq(ApiCredentialId.of(10L)), rule.capture());
        assertThat(rule.getValue()).isEqualTo(new TokenQuotaRule(0, QuotaScope.API_KEY, 10, "model-a",
                QuotaPeriod.DAY, new java.math.BigDecimal("500000.0"), true, QuotaSource.USER));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"model\":\"\",\"limitMillions\":1,\"enabled\":true}",
            "{\"model\":\"  \",\"limitMillions\":1,\"enabled\":true}",
            "{\"model\":\"model-a\",\"limitMillions\":0,\"enabled\":true}",
            "{\"model\":\"model-a\",\"limitMillions\":-1,\"enabled\":true}",
            "{\"model\":\"model-a\",\"limitMillions\":0.0000001,\"enabled\":true}",
            "{\"model\":\"model-a\",\"limitMillions\":1}"
    })
    void test_rejects_invalid_input_when_user_saves_quota(String body) throws Exception {
        // Arrange / Act / Assert
        mvc.perform(post("/api/api-credentials/10/token-quotas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
