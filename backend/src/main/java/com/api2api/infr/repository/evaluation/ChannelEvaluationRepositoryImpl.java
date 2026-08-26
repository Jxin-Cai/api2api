package com.api2api.infr.repository.evaluation;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.model.ChannelEvaluationId;
import com.api2api.domain.evaluation.repository.ChannelEvaluationRepository;
import com.api2api.domain.evaluation.repository.EvaluationHistoryQuery;
import com.api2api.domain.evaluation.repository.EvaluationScoreSummary;
import com.api2api.infr.repository.evaluation.converter.ChannelEvaluationPersistenceConverter;
import com.api2api.infr.repository.evaluation.mapper.ChannelEvaluationMapper;
import com.api2api.infr.repository.evaluation.po.ChannelEvaluationPO;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChannelEvaluationRepositoryImpl implements ChannelEvaluationRepository {

    @NonNull
    private final ChannelEvaluationMapper mapper;

    @NonNull
    private final ChannelEvaluationPersistenceConverter converter;

    @Override
    public void save(ChannelEvaluation evaluation) {
        Objects.requireNonNull(evaluation, "Channel evaluation must not be null");
        ChannelEvaluationPO po = converter.toPO(evaluation);
        if (mapper.selectById(po.getId()) == null) {
            mapper.insert(po);
        } else {
            mapper.update(po);
        }
    }

    @Override
    public Optional<ChannelEvaluation> findById(ChannelEvaluationId id) {
        Objects.requireNonNull(id, "Channel evaluation id must not be null");
        return Optional.ofNullable(mapper.selectById(id.value())).map(converter::toDomain);
    }

    @Override
    public List<ChannelEvaluation> findHistory(EvaluationHistoryQuery query) {
        Objects.requireNonNull(query, "Evaluation history query must not be null");
        return mapper.selectHistory(query).stream().map(converter::toDomain).toList();
    }

    @Override
    public long countHistory(EvaluationHistoryQuery query) {
        Objects.requireNonNull(query, "Evaluation history query must not be null");
        return mapper.countHistory(query);
    }

    @Override
    public EvaluationScoreSummary summarize(EvaluationHistoryQuery query) {
        Objects.requireNonNull(query, "Evaluation history query must not be null");
        return converter.toSummary(mapper.summarize(query));
    }

    @Override
    public List<ChannelEvaluation> findInFlight(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("In-flight evaluation limit must be positive");
        }
        return mapper.selectInFlight(limit).stream().map(converter::toDomain).toList();
    }

    @Override
    public int deleteByProviderChannelId(ProviderChannelId providerChannelId) {
        Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        return mapper.deleteByProviderChannelId(providerChannelId.value());
    }
}
