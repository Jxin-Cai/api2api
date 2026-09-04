package com.api2api.infr.repository.usage.mapper;

import com.api2api.infr.repository.usage.po.ModelGroupModelUsagePO;
import com.api2api.infr.repository.usage.po.UsageRecordPO;
import com.api2api.infr.repository.usage.po.UsageRecordQueryPO;
import com.api2api.infr.repository.usage.po.UsageTokenSummaryPO;
import java.time.Instant;
import java.util.List;
import java.math.BigDecimal;

public interface UsageRecordMapper {
    int insert(UsageRecordPO usageRecord);
    int updateById(UsageRecordPO usageRecord);
    int deleteById(Long id);
    UsageRecordPO selectById(Long id);
    UsageRecordPO selectByRequestId(String requestId);
    List<UsageRecordPO> selectByFilter(UsageRecordQueryPO query);
    long countByFilter(UsageRecordQueryPO query);
    long sumTotalTokensByApiCredential(Long apiCredentialId);
    BigDecimal sumActualTokensByApiCredential(Long apiCredentialId);
    BigDecimal sumActualTokensByModelGroupAndModel(Long modelGroupId, String requestedModel, Instant startInclusive, Instant endExclusive);
    List<ModelGroupModelUsagePO> sumActualTokensByOwnerGroupedByModel(Long ownerUserId, Instant startInclusive, Instant endExclusive);
    UsageTokenSummaryPO sumTokensByFilter(UsageRecordQueryPO query);
}
