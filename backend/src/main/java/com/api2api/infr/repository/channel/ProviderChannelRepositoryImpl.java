package com.api2api.infr.repository.channel;

import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.infr.repository.channel.converter.ProviderChannelPersistenceConverter;
import com.api2api.infr.repository.channel.mapper.ProviderChannelMapper;
import com.api2api.infr.repository.channel.po.ProviderChannelPO;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProviderChannelRepositoryImpl implements ProviderChannelRepository {

    @NonNull
    private final ProviderChannelMapper mapper;

    @NonNull
    private final ProviderChannelPersistenceConverter converter;

    @Override
    public void save(ProviderChannel providerChannel) {
        Objects.requireNonNull(providerChannel, "ProviderChannel must not be null");
        ProviderChannelPO po = converter.toPO(providerChannel);
        if (mapper.selectById(po.getId()) == null) {
            mapper.insert(po);
        } else {
            mapper.update(po);
        }
    }

    @Override
    public Optional<ProviderChannel> findById(ProviderChannelId id) {
        Objects.requireNonNull(id, "ProviderChannelId must not be null");
        return Optional.ofNullable(mapper.selectById(id.value()))
                .map(converter::toDomain);
    }

    @Override
    public List<ProviderChannel> findAll() {
        return mapper.selectAll().stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public List<ProviderChannel> findEnabledForRouting() {
        return mapper.selectEnabledForRouting().stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public void markModelRateLimited(
            ProviderChannelId id,
            ModelName upstreamModel,
            Instant limitedAt,
            Instant resetAt
    ) {
        Objects.requireNonNull(id, "ProviderChannelId must not be null");
        Objects.requireNonNull(upstreamModel, "Upstream model must not be null");
        Objects.requireNonNull(limitedAt, "Rate-limit time must not be null");
        Objects.requireNonNull(resetAt, "Rate-limit reset time must not be null");
        mapper.markModelRateLimited(id.value(), upstreamModel.value(), limitedAt, resetAt);
    }

    @Override
    public int restoreModelRateLimitsBefore(Instant now, Instant restoredAt) {
        Objects.requireNonNull(now, "Current time must not be null");
        Objects.requireNonNull(restoredAt, "Rate-limit restore time must not be null");
        return mapper.restoreModelRateLimitsBefore(now, restoredAt);
    }

    @Override
    public void softDeleteById(ProviderChannelId id, Instant deletedAt) {
        Objects.requireNonNull(id, "ProviderChannelId must not be null");
        Objects.requireNonNull(deletedAt, "Deleted time must not be null");
        mapper.softDeleteById(id.value(), deletedAt);
    }
}
