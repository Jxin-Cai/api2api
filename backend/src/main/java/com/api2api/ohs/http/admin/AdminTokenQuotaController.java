package com.api2api.ohs.http.admin;

import com.api2api.application.quota.TokenQuotaAdminService;
import com.api2api.domain.quota.*;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/token-quotas")
@RequiredArgsConstructor
public class AdminTokenQuotaController {
    private final TokenQuotaAdminService service;
    private final CurrentUserContextResolver users;

    @GetMapping
    public ApiResponse<TokenQuotaAdminService.QuotaPage> list(HttpServletRequest request) {
        return ApiResponse.success(service.list(users.resolveOperatorUserId(request)));
    }

    @PostMapping
    public ApiResponse<TokenQuotaAdminService.RuleView> create(@Valid @RequestBody QuotaRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.save(users.resolveOperatorUserId(request), body.rule(0)));
    }

    @PutMapping("/{id}")
    public ApiResponse<TokenQuotaAdminService.RuleView> update(@PathVariable long id,
            @Valid @RequestBody QuotaRequest body, HttpServletRequest request) {
        if (id <= 0) throw new IllegalArgumentException("Quota rule id must be positive");
        return ApiResponse.success(service.save(users.resolveOperatorUserId(request), body.rule(id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        service.delete(users.resolveOperatorUserId(request), id);
        return ApiResponse.success();
    }

    public record QuotaRequest(@NotNull QuotaScope scope, @NotNull @Positive Long targetId,
                               @Size(max = 256) String model, @NotNull QuotaPeriod period,
                               @NotNull @DecimalMin("0.000001") @Digits(integer = 9, fraction = 6) BigDecimal limitMillions,
                               @NotNull Boolean enabled) {
        TokenQuotaRule rule(long id) {
            return new TokenQuotaRule(id, scope, targetId, model, period, limitMillions.multiply(BigDecimal.valueOf(1_000_000)), enabled);
        }
    }
}
