package com.api2api.domain.quota;

import com.api2api.domain.usage.model.UsageTimeRange;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TokenQuotaRepository {
    List<TokenQuotaRule> findAll();
    Optional<TokenQuotaRule> findById(long id);
    List<TokenQuotaRule> findByTarget(QuotaScope scope, long targetId);
    List<TokenQuotaRule> findApplicable(long groupId, long keyId);
    TokenQuotaRule save(TokenQuotaRule rule);
    boolean delete(long id);
    boolean deleteUserRule(long id, long keyId);
    List<QuotaTarget> targets();
    boolean targetExists(QuotaScope scope, long targetId);
    void lockCredential(long keyId);
    void lockGroup(long groupId);
    BigDecimal consumed(TokenQuotaRule rule, UsageTimeRange window);
}
