package com.api2api.domain.usage.repository;

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
import com.api2api.domain.user.model.UserAccountId;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface UsageRecordRepository {

    /**
     * Persists a new usage record.
     *
     * <p>Used both for PENDING reservations (written atomically with the quota check) and,
     * for legacy compatibility, for direct SUCCESS/FAILED inserts.
     *
     * @param usageRecord record to persist; must not be null
     */
    void save(UsageRecord usageRecord);

    /**
     * Replaces a PENDING reservation with its final SUCCESS or FAILED state.
     *
     * <p>Implementations must UPDATE the existing row identified by {@link UsageRecord#id()}
     * rather than inserting a new row, so the PENDING token reservation is atomically replaced
     * by the actual consumption without leaving orphaned rows.
     *
     * @param usageRecord terminal (SUCCESS or FAILED) record with actual token usage
     */
    void update(UsageRecord usageRecord);

    /**
     * Removes a PENDING reservation created by a request that produced no billable usage.
     *
     * <p>Freeing the reservation immediately returns the reserved quota to the credential so
     * subsequent requests are not incorrectly blocked.
     *
     * @param id usage record id of the PENDING reservation to cancel
     */
    void cancelReservation(UsageRecordId id);

    /**
     * Loads a complete usage record by its identifier.
     * Implementations must include token details, protocol, channel, model dimensions and failure diagnostic data.
     * No matching record should be represented by {@link Optional#empty()}; invalid ids are rejected by the id value object.
     *
     * @param id usage record id
     * @return optional complete usage record
     */
    Optional<UsageRecord> findById(UsageRecordId id);

    /**
     * Loads a complete usage record by the gateway request tracing id.
     * This is used for idempotency checks and failure diagnostics. No matching record should be represented by
     * {@link Optional#empty()}; invalid request ids are rejected by the request id value object.
     *
     * @param requestId gateway request tracing id
     * @return optional complete usage record
     */
    Optional<UsageRecord> findByRequestId(GatewayRequestId requestId);

    /**
     * Queries usage records by filter and page request.
     * Implementations must apply the same role-based visibility constraints as the filter: regular users can query only
     * their own records, cannot filter by provider channel, and must receive channel-redacted records. Empty result sets
     * should be returned as an empty page with a zero known filtered token total.
     *
     * @param filter role-aware usage record filter
     * @param pageRequest page number and page size specification
     * @return page of usage records and filtered token totals
     */
    PagedUsageRecords query(UsageRecordFilter filter, PageRequestSpec pageRequest);

    /**
     * Sums total tokens for one API credential across all persisted usage records.
     * Implementations must use persisted usage facts as the source of truth and include successful records as well as
     * failed records that were stored according to business rules. No matching records should return {@code 0}.
     *
     * @param apiCredentialId API credential id
     * @return accumulated total tokens
     */
    long sumTotalTokensByApiCredential(ApiCredentialId apiCredentialId);

    /** Returns weighted tokens used by quota and statistical accounting. */
    BigDecimal sumActualTokensByApiCredential(ApiCredentialId apiCredentialId);

    /**
     * Sums weighted tokens of one requested model across every credential bound to a model group within
     * {@code timeRange}. PENDING reservations are included so concurrent requests observe in-flight commitments.
     */
    BigDecimal sumActualTokensByModelGroupAndModel(ModelGroupId modelGroupId, ModelName model, UsageTimeRange timeRange);

    /**
     * Aggregates weighted tokens per (model group, requested model) for every group owned by {@code ownerUserId}
     * within {@code timeRange}. Groups or models without usage are absent from the result.
     */
    List<ModelGroupModelUsage> sumActualTokensByOwnerGroupedByModel(UserAccountId ownerUserId, UsageTimeRange timeRange);

    /**
     * Sums token details using exactly the same filter and role-based visibility rules as {@link #query}.
     * Implementations must keep this aggregate result consistent with paged query totals. Empty result sets should return
     * a zero known {@link UsageTokenBreakdown}.
     *
     * @param filter role-aware usage record filter
     * @return filtered token detail total
     */
    UsageTokenBreakdown sumTokens(UsageRecordFilter filter);
}
