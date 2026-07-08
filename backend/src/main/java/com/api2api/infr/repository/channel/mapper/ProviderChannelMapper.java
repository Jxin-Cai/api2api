package com.api2api.infr.repository.channel.mapper;

import com.api2api.infr.repository.channel.po.ProviderChannelPO;
import java.util.List;

public interface ProviderChannelMapper {
    int insert(ProviderChannelPO providerChannel);
    int update(ProviderChannelPO providerChannel);
    ProviderChannelPO selectById(Long id);
    List<ProviderChannelPO> selectAll();
    List<ProviderChannelPO> selectEnabledForRouting();
    int softDeleteById(Long id, java.time.Instant updatedAt);
}
