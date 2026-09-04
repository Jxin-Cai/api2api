package com.api2api.infr.repository.usage;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.ModelGroupModelUsage;
import com.api2api.domain.usage.model.PageRequestSpec;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.domain.usage.model.UsageTimeRange;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.repository.usage.converter.UsageRecordPersistenceConverter;
import com.api2api.infr.repository.usage.mapper.UsageRecordMapper;
import com.api2api.infr.repository.usage.po.UsageRecordQueryPO;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.math.BigDecimal;
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
    public void update(UsageRecord usageRecord) {
        Objects.requireNonNull(usageRecord, "UsageRecord must not be null");
        mapper.updateById(converter.toPO(usageRecord));
    }

    @Override
    public void cancelReservation(UsageRecordId id) {
        Objects.requireNonNull(id, "UsageRecordId must not be null");
        mapper.deleteById(id.value());
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
    public BigDecimal sumActualTokensByApiCredential(ApiCredentialId apiCredentialId) {
        Objects.requireNonNull(apiCredentialId, "ApiCredentialId must not be null");
        return mapper.sumActualTokensByApiCredential(apiCredentialId.value());
    }

    @Override
    public BigDecimal sumActualTokensByModelGroupAndModel(
            ModelGroupId modelGroupId, ModelName model, UsageTimeRange timeRange) {
        Objects.requireNonNull(modelGroupId, "ModelGroupId must not be null");
        Objects.requireNonNull(model, "ModelName must not be null");
        Objects.requireNonNull(timeRange, "UsageTimeRange must not be null");
        return mapper.sumActualTokensByModelGroupAndModel(
                modelGroupId.value(), model.getValue(), timeRange.startInclusive(), timeRange.endExclusive());
    }

    @Override
    public List<ModelGroupModelUsage> sumActualTokensByOwnerGroupedByModel(
            UserAccountId ownerUserId, UsageTimeRange timeRange) {
        Objects.requireNonNull(ownerUserId, "UserAccountId must not be null");
        Objects.requireNonNull(timeRange, "UsageTimeRange must not be null");
        return mapper.sumActualTokensByOwnerGroupedByModel(
                        ownerUserId.getValue(), timeRange.startInclusive(), timeRange.endExclusive())
                .stream()
                .map(po -> new ModelGroupModelUsage(
                        ModelGroupId.of(po.getModelGroupId()),
                        ModelName.of(po.getRequestedModel()),
                        po.getActualTokens() == null ? BigDecimal.ZERO : po.getActualTokens()))
                .toList();
    }

    @Override
    public UsageTokenBreakdown sumTokens(UsageRecordFilter filter) {
        Objects.requireNonNull(filter, "UsageRecordFilter must not be null");
        return converter.toTokenBreakdown(mapper.sumTokensByFilter(converter.toQueryPO(filter)));
    }
}
