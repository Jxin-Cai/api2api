package com.api2api.infr.repository.channel.mapper;

import com.api2api.infr.repository.channel.po.ProviderChannelPO;
import java.util.List;

public interface ProviderChannelMapper {
    int insert(ProviderChannelPO providerChannel);
    int update(ProviderChannelPO providerChannel);
    ProviderChannelPO selectById(Long id);
    List<ProviderChannelPO> selectAll();
    List<ProviderChannelPO> selectEnabledForRouting();
    int markModelRateLimited(Long id, String upstreamModel, java.time.Instant limitedAt, java.time.Instant resetAt);
    int restoreModelRateLimitsBefore(java.time.Instant now, java.time.Instant updatedAt);
    int softDeleteById(Long id, java.time.Instant updatedAt);
}
