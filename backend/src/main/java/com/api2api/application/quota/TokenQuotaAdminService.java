package com.api2api.application.quota;

import com.api2api.application.BusinessException;
import com.api2api.domain.quota.*;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenQuotaAdminService {
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private final TokenQuotaRepository repository;
    private final UserAccountRepository users;
    private final TokenQuotaWindow windows;

    @Transactional(readOnly = true)
    public QuotaPage list(UserAccountId operator) {
        assertAdmin(operator);
        return new QuotaPage(repository.findAll().stream().map(this::view).toList(), repository.targets(), windows.zoneId());
    }

    @Transactional
    public RuleView save(UserAccountId operator, TokenQuotaRule rule) {
        assertAdmin(operator);
        if (!repository.targetExists(rule.scope(), rule.targetId())) throw new BusinessException("QUOTA_TARGET_NOT_FOUND", "限流对象不存在或已删除");
        TokenQuotaRule saved;
        try {
            saved = repository.save(rule);
        } catch (DuplicateKeyException duplicate) {
            throw new BusinessException("QUOTA_RULE_EXISTS", "该对象、模型和周期已有规则，请编辑现有规则", duplicate);
        }
        if (saved == null) throw new BusinessException("QUOTA_RULE_NOT_FOUND", "限流规则不存在，请刷新后重试");
        return view(saved);
    }

    @Transactional
    public void delete(UserAccountId operator, long id) {
        assertAdmin(operator);
        if (!repository.delete(id)) throw new BusinessException("QUOTA_RULE_NOT_FOUND", "限流规则不存在，请刷新后重试");
    }

    private RuleView view(TokenQuotaRule rule) {
        var window = windows.current(rule.period());
        BigDecimal consumed = repository.consumed(rule, window);
        return new RuleView(Long.toString(rule.id()), rule.scope(), Long.toString(rule.targetId()), rule.model(), rule.period(),
                rule.limitTokens().divide(MILLION), rule.enabled(), consumed.divide(MILLION),
                rule.limitTokens().subtract(consumed).max(BigDecimal.ZERO).divide(MILLION),
                rule.enabled() && consumed.compareTo(rule.limitTokens()) >= 0,
                rule.period() == QuotaPeriod.LIFETIME ? null : window.endExclusive());
    }

    private void assertAdmin(UserAccountId operator) {
        users.findById(operator).orElseThrow(() -> new BusinessException("USER_NOT_FOUND"))
                .assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }

    public record RuleView(String id, QuotaScope scope, String targetId, String model, QuotaPeriod period,
                           BigDecimal limitMillions, boolean enabled, BigDecimal usedMillions, BigDecimal remainingMillions,
                           boolean limited, Instant resetAt) {}
    public record QuotaPage(List<RuleView> rules, List<QuotaTarget> targets, String zoneId) {}
}
