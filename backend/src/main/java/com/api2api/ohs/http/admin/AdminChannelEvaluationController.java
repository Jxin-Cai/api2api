package com.api2api.ohs.http.admin;

import com.api2api.application.evaluation.ChannelEvaluationApplicationService;
import com.api2api.application.evaluation.command.LoadChannelEvaluationScheduleCommand;
import com.api2api.application.evaluation.command.QueryChannelEvaluationHistoryCommand;
import com.api2api.application.evaluation.command.SubmitChannelEvaluationCommand;
import com.api2api.application.evaluation.command.UpsertChannelEvaluationScheduleCommand;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.evaluation.model.ChannelEvaluation;
import com.api2api.domain.evaluation.model.ChannelEvaluationSchedule;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.admin.converter.ChannelEvaluationHttpConverter;
import com.api2api.ohs.http.admin.dto.AdminSubmitChannelEvaluationRequest;
import com.api2api.ohs.http.admin.dto.AdminUpsertChannelEvaluationScheduleRequest;
import com.api2api.ohs.http.admin.dto.ChannelEvaluationHistoryResponse;
import com.api2api.ohs.http.admin.dto.ChannelEvaluationScheduleResponse;
import com.api2api.ohs.http.admin.dto.ChannelEvaluationSubmitResponse;
import com.api2api.ohs.http.admin.dto.QueryChannelEvaluationHistoryRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for channel evaluation runs and schedules.
 */
@RestController
@RequestMapping("/api/admin/provider-channels/{provider-channel-id}/evaluations")
@Validated
@RequiredArgsConstructor
public class AdminChannelEvaluationController {

    @NonNull
    private final ChannelEvaluationApplicationService channelEvaluationApplicationService;

    @NonNull
    private final ChannelEvaluationHttpConverter channelEvaluationHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @PostMapping
    public ApiResponse<ChannelEvaluationSubmitResponse> submit(
            @PathVariable("provider-channel-id") Long providerChannelId,
            @Valid @RequestBody(required = false) AdminSubmitChannelEvaluationRequest submitRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        SubmitChannelEvaluationCommand command = channelEvaluationHttpConverter.toSubmitCommand(
                submitRequest,
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        List<ChannelEvaluation> evaluations = channelEvaluationApplicationService.submit(command);
        return ApiResponse.success(channelEvaluationHttpConverter.toSubmitResponse(evaluations));
    }

    @GetMapping
    public ApiResponse<ChannelEvaluationHistoryResponse> history(
            @PathVariable("provider-channel-id") Long providerChannelId,
            @Valid QueryChannelEvaluationHistoryRequest queryRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        QueryChannelEvaluationHistoryCommand command = channelEvaluationHttpConverter.toHistoryCommand(
                queryRequest,
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        return ApiResponse.success(channelEvaluationHttpConverter.toHistoryResponse(
                channelEvaluationApplicationService.history(command)
        ));
    }

    @GetMapping("/schedule")
    public ApiResponse<ChannelEvaluationScheduleResponse> loadSchedule(
            @PathVariable("provider-channel-id") Long providerChannelId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        LoadChannelEvaluationScheduleCommand command = channelEvaluationHttpConverter.toLoadScheduleCommand(
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        ChannelEvaluationSchedule schedule = channelEvaluationApplicationService.loadSchedule(command).orElse(null);
        return ApiResponse.success(channelEvaluationHttpConverter.toScheduleResponse(schedule));
    }

    @PutMapping("/schedule")
    public ApiResponse<ChannelEvaluationScheduleResponse> upsertSchedule(
            @PathVariable("provider-channel-id") Long providerChannelId,
            @Valid @RequestBody AdminUpsertChannelEvaluationScheduleRequest upsertRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UpsertChannelEvaluationScheduleCommand command = channelEvaluationHttpConverter.toUpsertScheduleCommand(
                upsertRequest,
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        ChannelEvaluationSchedule schedule = channelEvaluationApplicationService.upsertSchedule(command);
        return ApiResponse.success(channelEvaluationHttpConverter.toScheduleResponse(schedule));
    }
}
