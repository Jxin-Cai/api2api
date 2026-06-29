package com.api2api.infr.repository.usage;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.PageRequestSpec;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.infr.repository.usage.converter.UsageRecordPersistenceConverter;
import com.api2api.infr.repository.usage.mapper.UsageRecordMapper;
import com.api2api.infr.repository.usage.po.UsageRecordQueryPO;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UsageRecordRepositoryImpl implements UsageRecordRepository {

    @NonNull
    private final UsageRecordMapper mapper;

    @NonNull
    private final UsageRecordPersistenceConverter converter;

    @Override
    public void save(UsageRecord usageRecord) {
        Objects.requireNonNull(usageRecord, "UsageRecord must not be null");
        mapper.insert(converter.toPO(usageRecord));
    }

    @Override
    public Optional<UsageRecord> findById(UsageRecordId id) {
        Objects.requireNonNull(id, "UsageRecordId must not be null");
        return Optional.ofNullable(mapper.selectById(id.value()))
                .map(converter::toDomain);
    }

    @Override
    public Optional<UsageRecord> findByRequestId(GatewayRequestId requestId) {
        Objects.requireNonNull(requestId, "GatewayRequestId must not be null");
        return Optional.ofNullable(mapper.selectByRequestId(requestId.value()))
                .map(converter::toDomain);
    }

    @Override
    public PagedUsageRecords query(UsageRecordFilter filter, PageRequestSpec pageRequest) {
        Objects.requireNonNull(filter, "UsageRecordFilter must not be null");
        Objects.requireNonNull(pageRequest, "PageRequestSpec must not be null");
        UsageRecordQueryPO query = converter.toQueryPO(filter, pageRequest);
        long total = mapper.countByFilter(query);
        return converter.toPage(mapper.selectByFilter(query), pageRequest, total, mapper.sumTokensByFilter(query));
    }

    @Override
    public long sumTotalTokensByApiCredential(ApiCredentialId apiCredentialId) {
        Objects.requireNonNull(apiCredentialId, "ApiCredentialId must not be null");
        return mapper.sumTotalTokensByApiCredential(apiCredentialId.value());
    }

    @Override
    public UsageTokenBreakdown sumTokens(UsageRecordFilter filter) {
        Objects.requireNonNull(filter, "UsageRecordFilter must not be null");
        return converter.toTokenBreakdown(mapper.sumTokensByFilter(converter.toQueryPO(filter)));
    }
}
