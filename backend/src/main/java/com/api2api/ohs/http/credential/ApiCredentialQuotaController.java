package com.api2api.ohs.http.credential;

import com.api2api.application.quota.TokenQuotaManagementService;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.quota.*;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/api-credentials/{keyId}/token-quotas")
@RequiredArgsConstructor
@Validated
public class ApiCredentialQuotaController {
    private final TokenQuotaManagementService service;
    private final CurrentUserContextResolver users;

    @GetMapping
    public ApiResponse<TokenQuotaManagementService.QuotaPage> list(@PathVariable @Positive long keyId,
            HttpServletRequest request) {
        return ApiResponse.success(service.listForCredential(users.resolveCurrentUserId(request), ApiCredentialId.of(keyId)));
    }

    @PostMapping
    public ApiResponse<TokenQuotaManagementService.RuleView> create(@PathVariable @Positive long keyId,
            @Valid @RequestBody DailyQuotaRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.saveForCredential(users.resolveCurrentUserId(request), ApiCredentialId.of(keyId), body.rule(0, keyId)));
    }

    @PutMapping("/{ruleId}")
    public ApiResponse<TokenQuotaManagementService.RuleView> update(@PathVariable @Positive long keyId,
            @PathVariable @Positive long ruleId, @Valid @RequestBody DailyQuotaRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.saveForCredential(users.resolveCurrentUserId(request), ApiCredentialId.of(keyId), body.rule(ruleId, keyId)));
    }

    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> delete(@PathVariable @Positive long keyId, @PathVariable @Positive long ruleId,
            HttpServletRequest request) {
        service.deleteForCredential(users.resolveCurrentUserId(request), ApiCredentialId.of(keyId), ruleId);
        return ApiResponse.success();
    }

    /** Scope, source, period and target are fixed by this endpoint, never accepted from client input. */
    public record DailyQuotaRequest(@NotBlank @Size(max = 256) String model,
                                    @NotNull @DecimalMin("0.000001") @Digits(integer = 9, fraction = 6) BigDecimal limitMillions,
                                    @NotNull Boolean enabled) {
        TokenQuotaRule rule(long id, long keyId) {
            return new TokenQuotaRule(id, QuotaScope.API_KEY, keyId, model, QuotaPeriod.DAY,
                    limitMillions.multiply(BigDecimal.valueOf(1_000_000)), enabled, QuotaSource.USER);
        }
    }
}
