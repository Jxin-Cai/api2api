package com.api2api.infr.repository.evaluation;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.ChannelEvaluationSchedule;
import com.api2api.domain.evaluation.model.ChannelEvaluationScheduleId;
import com.api2api.domain.evaluation.repository.ChannelEvaluationScheduleRepository;
import com.api2api.infr.repository.evaluation.converter.ChannelEvaluationPersistenceConverter;
import com.api2api.infr.repository.evaluation.mapper.ChannelEvaluationScheduleMapper;
import com.api2api.infr.repository.evaluation.po.ChannelEvaluationSchedulePO;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChannelEvaluationScheduleRepositoryImpl implements ChannelEvaluationScheduleRepository {

    @NonNull
    private final ChannelEvaluationScheduleMapper mapper;

    @NonNull
    private final ChannelEvaluationPersistenceConverter converter;

    @Override
    public void save(ChannelEvaluationSchedule schedule) {
        Objects.requireNonNull(schedule, "Channel evaluation schedule must not be null");
        ChannelEvaluationSchedulePO po = converter.toSchedulePO(schedule);
        if (mapper.selectById(po.getId()) == null) {
            mapper.insert(po);
        } else {
            mapper.update(po);
        }
    }

    @Override
    public Optional<ChannelEvaluationSchedule> findById(ChannelEvaluationScheduleId id) {
        Objects.requireNonNull(id, "Channel evaluation schedule id must not be null");
        return Optional.ofNullable(mapper.selectById(id.value())).map(converter::toScheduleDomain);
    }

    @Override
    public Optional<ChannelEvaluationSchedule> findByProviderChannelId(ProviderChannelId providerChannelId) {
        Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        return Optional.ofNullable(mapper.selectByProviderChannelId(providerChannelId.value()))
                .map(converter::toScheduleDomain);
    }

    @Override
    public List<ChannelEvaluationSchedule> findDue(Instant now, int limit) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("Due schedule limit must be positive");
        }
        return mapper.selectDue(now, limit).stream().map(converter::toScheduleDomain).toList();
    }

    @Override
    public int deleteByProviderChannelId(ProviderChannelId providerChannelId) {
        Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        return mapper.deleteByProviderChannelId(providerChannelId.value());
    }
}
