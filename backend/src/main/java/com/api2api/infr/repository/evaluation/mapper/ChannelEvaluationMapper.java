package com.api2api.infr.repository.evaluation.mapper;

import com.api2api.domain.evaluation.repository.EvaluationHistoryQuery;
import com.api2api.infr.repository.evaluation.po.ChannelEvaluationPO;
import com.api2api.infr.repository.evaluation.po.EvaluationScoreSummaryPO;
import java.util.List;

public interface ChannelEvaluationMapper {

    int insert(ChannelEvaluationPO evaluation);

    int update(ChannelEvaluationPO evaluation);

    ChannelEvaluationPO selectById(Long id);

    List<ChannelEvaluationPO> selectHistory(EvaluationHistoryQuery query);

    long countHistory(EvaluationHistoryQuery query);

    EvaluationScoreSummaryPO summarize(EvaluationHistoryQuery query);

    List<ChannelEvaluationPO> selectInFlight(int limit);

    int deleteByProviderChannelId(Long providerChannelId);
}
