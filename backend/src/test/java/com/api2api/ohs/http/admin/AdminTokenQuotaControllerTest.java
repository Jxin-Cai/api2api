package com.api2api.ohs.http.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.api2api.application.quota.TokenQuotaManagementService;
import com.api2api.domain.quota.*;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.GlobalExceptionAdvice;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminTokenQuotaControllerTest {
    private final TokenQuotaManagementService service = mock(TokenQuotaManagementService.class);
    private final CurrentUserContextResolver users = mock(CurrentUserContextResolver.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminTokenQuotaController(service, users))
            .setControllerAdvice(new GlobalExceptionAdvice()).build();

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT" })
    void test_rejects_key_scope_when_admin_submits_quota(String method) throws Exception {
        // Arrange
        String body = """
                {"scope":"API_KEY","targetId":10,"model":"model-a",
                 "period":"DAY","limitMillions":1,"enabled":true}
                """;
        String path = "/api/admin/token-quotas" + (method.equals("PUT") ? "/30" : "");
        // Act / Assert
        mvc.perform(request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @EnumSource(QuotaPeriod.class)
    void test_saves_group_quota_when_admin_selects_supported_period(QuotaPeriod period) throws Exception {
        // Arrange
        when(users.resolveOperatorUserId(any())).thenReturn(UserAccountId.of(1L));
        String body = """
                {"scope":"MODEL_GROUP","targetId":20,"model":"model-a",
                 "period":"%s","limitMillions":0.5,"enabled":true,"source":"USER"}
                """.formatted(period);
        // Act
        mvc.perform(post("/api/admin/token-quotas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        // Assert
        var rule = ArgumentCaptor.forClass(TokenQuotaRule.class);
        verify(service).save(eq(UserAccountId.of(1L)), rule.capture());
        assertThat(rule.getValue()).isEqualTo(new TokenQuotaRule(0, QuotaScope.MODEL_GROUP, 20, "model-a",
                period, new java.math.BigDecimal("500000.0"), true, QuotaSource.ADMIN));
    }
}
