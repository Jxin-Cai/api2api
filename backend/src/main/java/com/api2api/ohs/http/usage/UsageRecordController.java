package com.api2api.ohs.http.usage;

import com.api2api.application.usage.UsageQueryApplicationService;
import com.api2api.application.usage.command.QueryAdminUsageRecordsCommand;
import com.api2api.application.usage.command.QueryMyUsageRecordsCommand;
import com.api2api.application.usage.dto.PagedUsageRecordViews;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.usage.converter.UsageRecordHttpConverter;
import com.api2api.ohs.http.usage.dto.QueryAdminUsageRecordsRequest;
import com.api2api.ohs.http.usage.dto.QueryMyUsageRecordsRequest;
import com.api2api.ohs.http.usage.dto.UsageRecordPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for usage record queries (frontend and admin).
 */
@RestController
@Validated
@RequiredArgsConstructor
public class UsageRecordController {

    @NonNull
    private final UsageQueryApplicationService usageQueryApplicationService;

    @NonNull
    private final UsageRecordHttpConverter usageRecordHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @GetMapping("/api/usage-records")
    public ApiResponse<UsageRecordPageResponse> queryMyUsageRecords(
            @Valid QueryMyUsageRecordsRequest queryRequest,
            HttpServletRequest request
    ) {
        UserAccountId currentUserId = currentUserContextResolver.resolveCurrentUserId(request);
        QueryMyUsageRecordsCommand command = usageRecordHttpConverter.toQueryMyCommand(queryRequest, currentUserId);
        PagedUsageRecordViews pagedRecords = usageQueryApplicationService.queryMyUsageRecords(command);
        return ApiResponse.success(usageRecordHttpConverter.toPageResponse(pagedRecords, false));
    }

    @GetMapping("/api/admin/usage-records")
    public ApiResponse<UsageRecordPageResponse> queryAdminUsageRecords(
            @Valid QueryAdminUsageRecordsRequest queryRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        QueryAdminUsageRecordsCommand command = usageRecordHttpConverter.toQueryAdminCommand(queryRequest, operatorUserId);
        PagedUsageRecordViews pagedRecords = usageQueryApplicationService.queryAdminUsageRecords(command);
        return ApiResponse.success(usageRecordHttpConverter.toPageResponse(pagedRecords, true));
    }
}
