package com.api2api.infr.repository.usage.mapper;

import com.api2api.infr.repository.usage.po.UsageRecordPO;
import com.api2api.infr.repository.usage.po.UsageRecordQueryPO;
import com.api2api.infr.repository.usage.po.UsageTokenSummaryPO;
import java.util.List;

public interface UsageRecordMapper {
    int insert(UsageRecordPO usageRecord);
    UsageRecordPO selectById(Long id);
    UsageRecordPO selectByRequestId(String requestId);
    List<UsageRecordPO> selectByFilter(UsageRecordQueryPO query);
    long countByFilter(UsageRecordQueryPO query);
    long sumTotalTokensByApiCredential(Long apiCredentialId);
    UsageTokenSummaryPO sumTokensByFilter(UsageRecordQueryPO query);
}
