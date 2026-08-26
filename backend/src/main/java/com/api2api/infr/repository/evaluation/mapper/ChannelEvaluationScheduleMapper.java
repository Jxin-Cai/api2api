package com.api2api.infr.repository.evaluation.mapper;

import com.api2api.infr.repository.evaluation.po.ChannelEvaluationSchedulePO;
import java.time.Instant;
import java.util.List;

public interface ChannelEvaluationScheduleMapper {

    int insert(ChannelEvaluationSchedulePO schedule);

    int update(ChannelEvaluationSchedulePO schedule);

    ChannelEvaluationSchedulePO selectById(Long id);

    ChannelEvaluationSchedulePO selectByProviderChannelId(Long providerChannelId);

    List<ChannelEvaluationSchedulePO> selectDue(Instant now, int limit);

    int deleteByProviderChannelId(Long providerChannelId);
}
