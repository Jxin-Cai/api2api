package com.api2api.application.quota;

import com.api2api.application.BusinessException;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.quota.*;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenQuotaManagementService {
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private final TokenQuotaRepository repository;
    private final UserAccountRepository users;
    private final TokenQuotaWindow windows;
    private final ApiCredentialRepository credentials;

    @Transactional(readOnly = true)
    public QuotaPage list(UserAccountId operator) {
        assertAdmin(operator);
        return new QuotaPage(repository.findAll().stream().filter(this::isAdminGroupRule).map(this::view).toList(),
                repository.targets(), windows.zoneId());
    }

    @Transactional(readOnly = true)
    public QuotaPage listForCredential(UserAccountId operator, ApiCredentialId keyId) {
        var credential = ownedCredential(operator, keyId);
        var keyRules = repository.findByTarget(QuotaScope.API_KEY, keyId.value());
        var groupRules = repository.findByTarget(QuotaScope.MODEL_GROUP, credential.getModelGroupId().value());
        return new QuotaPage(Stream.concat(keyRules.stream(), groupRules.stream()).map(this::view).toList(),
                List.of(), windows.zoneId());
    }

    @Transactional
    public RuleView save(UserAccountId operator, TokenQuotaRule rule) {
        assertAdmin(operator);
        if (!isAdminGroupRule(rule)) {
            throw new BusinessException("QUOTA_SCOPE_NOT_ALLOWED", "管理员仅可配置模型分组额度，Key 每日上限由用户在前台设置");
        }
        if (rule.id() != 0) loadAdminGroupRule(rule.id());
        if (!repository.targetExists(rule.scope(), rule.targetId())) throw new BusinessException("QUOTA_TARGET_NOT_FOUND", "限流对象不存在或已删除");
        return saveRule(rule);
    }

    @Transactional
    public RuleView saveForCredential(UserAccountId operator, ApiCredentialId keyId, TokenQuotaRule rule) {
        if (rule.source() != QuotaSource.USER || rule.targetId() != keyId.value()) {
            throw new IllegalArgumentException("User quota must belong to the requested key");
        }
        repository.lockCredential(keyId.value());
        var credential = ownedCredential(operator, keyId);
        if (rule.enabled()) credential.assertModelAllowed(ModelName.of(rule.model()));
        return saveRule(rule);
    }

    @Transactional
    public void deleteForCredential(UserAccountId operator, ApiCredentialId keyId, long ruleId) {
        repository.lockCredential(keyId.value());
        ownedCredential(operator, keyId);
        if (!repository.deleteUserRule(ruleId, keyId.value())) {
            throw new BusinessException("QUOTA_RULE_NOT_FOUND", "用户自设规则不存在，请刷新后重试");
        }
    }

    private ApiCredential ownedCredential(UserAccountId operator, ApiCredentialId keyId) {
        users.findById(operator).orElseThrow(() -> new BusinessException("USER_NOT_FOUND"))
                .assertCanAccess(AccessScope.USER_PORTAL);
        var credential = credentials.findById(keyId).orElseThrow(() -> new BusinessException("API_CREDENTIAL_NOT_FOUND"));
        credential.assertOwnedBy(operator);
        return credential;
    }

    private RuleView saveRule(TokenQuotaRule rule) {
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
        loadAdminGroupRule(id);
        if (!repository.delete(id)) throw new BusinessException("QUOTA_RULE_NOT_FOUND", "限流规则不存在，请刷新后重试");
    }

    private TokenQuotaRule loadAdminGroupRule(long id) {
        return repository.findById(id).filter(this::isAdminGroupRule)
                .orElseThrow(() -> new BusinessException("QUOTA_RULE_NOT_FOUND", "模型分组限流规则不存在，请刷新后重试"));
    }

    private boolean isAdminGroupRule(TokenQuotaRule rule) {
        return rule.scope() == QuotaScope.MODEL_GROUP && rule.source() == QuotaSource.ADMIN;
    }

    private RuleView view(TokenQuotaRule rule) {
        var window = windows.current(rule.period());
        BigDecimal consumed = repository.consumed(rule, window);
        return new RuleView(Long.toString(rule.id()), rule.scope(), Long.toString(rule.targetId()), rule.model(), rule.period(),
                rule.limitTokens().divide(MILLION), rule.enabled(), consumed.divide(MILLION),
                rule.limitTokens().subtract(consumed).max(BigDecimal.ZERO).divide(MILLION),
                rule.enabled() && consumed.compareTo(rule.limitTokens()) >= 0,
                rule.period() == QuotaPeriod.LIFETIME ? null : window.endExclusive(), rule.source());
    }

    private void assertAdmin(UserAccountId operator) {
        users.findById(operator).orElseThrow(() -> new BusinessException("USER_NOT_FOUND"))
                .assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }

    public record RuleView(String id, QuotaScope scope, String targetId, String model, QuotaPeriod period,
                           BigDecimal limitMillions, boolean enabled, BigDecimal usedMillions, BigDecimal remainingMillions,
                           boolean limited, Instant resetAt, QuotaSource source) {}
    public record QuotaPage(List<RuleView> rules, List<QuotaTarget> targets, String zoneId) {}
}
