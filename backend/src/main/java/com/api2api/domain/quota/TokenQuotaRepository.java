package com.api2api.domain.quota;

import com.api2api.domain.usage.model.UsageTimeRange;
import java.math.BigDecimal;
import java.util.List;

public interface TokenQuotaRepository {
    List<TokenQuotaRule> findAll();
    List<TokenQuotaRule> findApplicable(long groupId, long keyId);
    TokenQuotaRule save(TokenQuotaRule rule);
    boolean delete(long id);
    List<QuotaTarget> targets();
    boolean targetExists(QuotaScope scope, long targetId);
    void lockCredential(long keyId);
    void lockGroup(long groupId);
    BigDecimal consumed(TokenQuotaRule rule, UsageTimeRange window);
}
