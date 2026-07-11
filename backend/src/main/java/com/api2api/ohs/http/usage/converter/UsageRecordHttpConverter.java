package com.api2api.ohs.http.usage.converter;

import com.api2api.application.usage.command.QueryAdminUsageRecordsCommand;
import com.api2api.application.usage.command.QueryMyUsageRecordsCommand;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.application.usage.dto.PagedUsageRecordViews;
import com.api2api.application.usage.dto.UsageRecordView;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.usage.dto.QueryAdminUsageRecordsRequest;
import com.api2api.ohs.http.usage.dto.QueryMyUsageRecordsRequest;
import com.api2api.ohs.http.usage.dto.UsageRecordPageResponse;
import com.api2api.ohs.http.usage.dto.UsageRecordResponse;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts usage HTTP query models to application commands and responses.
 */
@Mapper(config = MapStructConfig.class)
public abstract class UsageRecordHttpConverter {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 50;

    public QueryMyUsageRecordsCommand toQueryMyCommand(
            QueryMyUsageRecordsRequest request,
            UserAccountId currentUserId
    ) {
        return QueryMyUsageRecordsCommand.builder()
                .currentUserId(currentUserId)
                .apiCredentialId(toApiCredentialId(request.getApiCredentialId()))
                .requestedModel(toModelName(request.getRequestedModel()))
                .requestProtocol(toProtocolType(request.getRequestProtocol()))
                .startInclusive(defaultStart(request.getStartInclusive()))
                .endExclusive(defaultEnd(request.getEndExclusive()))
                .page(defaultPage(request.getPage()))
                .size(defaultSize(request.getSize()))
                .build();
    }

    public QueryAdminUsageRecordsCommand toQueryAdminCommand(
            QueryAdminUsageRecordsRequest request,
            UserAccountId operatorUserId
    ) {
        return QueryAdminUsageRecordsCommand.builder()
                .operatorUserId(operatorUserId)
                .userAccountId(toUserAccountId(request.getUserAccountId()))
                .apiCredentialId(toApiCredentialId(request.getApiCredentialId()))
                .requestedModel(toModelName(request.getRequestedModel()))
                .providerChannelId(toProviderChannelId(request.getProviderChannelId()))
                .requestProtocol(toProtocolType(request.getRequestProtocol()))
                .startInclusive(defaultStart(request.getStartInclusive()))
                .endExclusive(defaultEnd(request.getEndExclusive()))
                .page(defaultPage(request.getPage()))
                .size(defaultSize(request.getSize()))
                .build();
    }

    public UsageRecordPageResponse toPageResponse(PagedUsageRecordViews pagedRecords, boolean adminView) {
        return UsageRecordPageResponse.builder()
                .records(pagedRecords.getRecords().stream().map(this::toRecordResponse).toList())
                .page(pagedRecords.getPage())
                .size(pagedRecords.getSize())
                .totalElements(pagedRecords.getTotalElements())
                .totalPages(pagedRecords.totalPages())
                .filteredTotalTokens(pagedRecords.getFilteredTokenTotal().getTotalTokens())
                .adminView(adminView)
                .build();
    }

    protected UsageRecordResponse toRecordResponse(UsageRecordView view) {
        UsageRecordResponse response = toRecordResponse(view.getRecord());
        response.setUsername(view.getUsername());
        response.setApiCredentialName(view.getApiCredentialName());
        response.setProviderChannelName(view.getProviderChannelName());
        return response;
    }

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "requestId", source = "requestId.value")
    @Mapping(target = "userAccountId", source = "userAccountId.value")
    @Mapping(target = "apiCredentialId", source = "apiCredentialId.value")
    @Mapping(target = "requestedModel", expression = "java(modelNameValue(record.getRequestedModel()))")
    @Mapping(target = "upstreamModel", expression = "java(modelNameValue(record.getUpstreamModel()))")
    @Mapping(target = "providerChannelId", expression = "java(providerChannelIdValue(record.getProviderChannelId()))")
    @Mapping(target = "inputTokens", source = "tokenUsage.inputTokens")
    @Mapping(target = "outputTokens", source = "tokenUsage.outputTokens")
    @Mapping(target = "cacheCreationInputTokens", source = "tokenUsage.cacheCreationInputTokens")
    @Mapping(target = "cacheReadInputTokens", source = "tokenUsage.cacheReadInputTokens")
    @Mapping(target = "totalTokens", source = "tokenUsage.totalTokens")
    @Mapping(target = "usageKnown", source = "tokenUsage.usageKnown")
    @Mapping(target = "errorType", expression = "java(errorTypeValue(record))")
    @Mapping(target = "errorMessage", expression = "java(errorMessageValue(record))")
    protected abstract UsageRecordResponse toRecordResponse(UsageRecord record);

    protected String modelNameValue(ModelName modelName) {
        return modelName == null ? null : modelName.value();
    }

    protected Long providerChannelIdValue(ProviderChannelId providerChannelId) {
        return providerChannelId == null ? null : providerChannelId.value();
    }

    protected String errorTypeValue(UsageRecord record) {
        return record.getErrorDiagnostic() == null ? null : record.getErrorDiagnostic().getErrorType().name();
    }

    protected String errorMessageValue(UsageRecord record) {
        return record.getErrorDiagnostic() == null ? null : record.getErrorDiagnostic().getMessage();
    }

    protected UserAccountId toUserAccountId(Long value) {
        return value == null ? null : UserAccountId.of(value);
    }

    protected ApiCredentialId toApiCredentialId(Long value) {
        return value == null ? null : ApiCredentialId.of(value);
    }

    protected ProviderChannelId toProviderChannelId(Long value) {
        return value == null ? null : ProviderChannelId.of(value);
    }

    protected ModelName toModelName(String value) {
        return value == null || value.isBlank() ? null : ModelName.of(value);
    }

    protected ProtocolType toProtocolType(String value) {
        return value == null || value.isBlank()
                ? null
                : ProtocolType.parseExternal(value).orElse(null);
    }

    private Instant defaultStart(Instant value) {
        return value == null ? Instant.EPOCH : value;
    }

    private Instant defaultEnd(Instant value) {
        return value == null ? Instant.now() : value;
    }

    private int defaultPage(Integer value) {
        return value == null ? DEFAULT_PAGE : value;
    }

    private int defaultSize(Integer value) {
        int size = value == null ? DEFAULT_SIZE : value;
        if (size != 50 && size != 100 && size != 200) {
            throw new IllegalArgumentException("Usage page size must be one of 50, 100 or 200");
        }
        return size;
    }
}
