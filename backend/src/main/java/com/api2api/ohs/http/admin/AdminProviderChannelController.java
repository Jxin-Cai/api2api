package com.api2api.ohs.http.admin;

import com.api2api.application.channel.ProviderChannelApplicationService;
import com.api2api.application.channel.command.ChangeProviderChannelStatusCommand;
import com.api2api.application.channel.command.CreateProviderChannelCommand;
import com.api2api.application.channel.command.FetchProviderModelPreviewCommand;
import com.api2api.application.channel.command.FetchProviderModelsCommand;
import com.api2api.application.channel.command.RemoveChannelModelCommand;
import com.api2api.application.channel.command.UpdateProviderChannelCommand;
import com.api2api.application.channel.command.UpsertChannelModelCommand;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.IdentifierFactory;
import com.api2api.ohs.http.admin.converter.ProviderChannelHttpConverter;
import com.api2api.ohs.http.admin.dto.AdminCreateProviderChannelRequest;
import com.api2api.ohs.http.admin.dto.AdminFetchProviderModelPreviewRequest;
import com.api2api.ohs.http.admin.dto.AdminFetchProviderModelsRequest;
import com.api2api.ohs.http.admin.dto.AdminUpdateProviderChannelRequest;
import com.api2api.ohs.http.admin.dto.AdminUpsertChannelModelRequest;
import com.api2api.ohs.http.admin.dto.ProviderChannelListResponse;
import com.api2api.ohs.http.admin.dto.ProviderChannelResponse;
import com.api2api.ohs.http.admin.dto.ProviderModelPreviewResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for provider channel management.
 */
@RestController
@RequestMapping("/api/admin/provider-channels")
@Validated
@RequiredArgsConstructor
public class AdminProviderChannelController {

    @NonNull
    private final ProviderChannelApplicationService providerChannelApplicationService;

    @NonNull
    private final ProviderChannelHttpConverter providerChannelHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @NonNull
    private final IdentifierFactory identifierFactory;

    @GetMapping
    public ApiResponse<ProviderChannelListResponse> listChannels(HttpServletRequest request) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        List<ProviderChannel> channels = providerChannelApplicationService.listChannels(operatorUserId);
        return ApiResponse.success(providerChannelHttpConverter.toListResponse(channels));
    }

    @PostMapping
    public ApiResponse<ProviderChannelResponse> createChannel(
            @Valid @RequestBody AdminCreateProviderChannelRequest createRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ProviderChannelId providerChannelId = identifierFactory.newProviderChannelId();
        CreateProviderChannelCommand command = providerChannelHttpConverter.toCreateCommand(
                createRequest, operatorUserId, providerChannelId);
        ProviderChannel channel = providerChannelApplicationService.createChannel(command);
        return ApiResponse.success(providerChannelHttpConverter.toResponse(channel));
    }

    @PutMapping("/{provider-channel-id}")
    public ApiResponse<ProviderChannelResponse> updateChannel(
            @PathVariable("provider-channel-id") Long providerChannelId,
            @Valid @RequestBody AdminUpdateProviderChannelRequest updateRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UpdateProviderChannelCommand command = providerChannelHttpConverter.toUpdateCommand(
                updateRequest,
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        ProviderChannel channel = providerChannelApplicationService.updateChannel(command);
        return ApiResponse.success(providerChannelHttpConverter.toResponse(channel));
    }

    @PatchMapping("/{provider-channel-id}/enable")
    public ApiResponse<ProviderChannelResponse> enableChannel(
            @PathVariable("provider-channel-id") Long providerChannelId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ChangeProviderChannelStatusCommand command = providerChannelHttpConverter.toChangeStatusCommand(
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        ProviderChannel channel = providerChannelApplicationService.enableChannel(command);
        return ApiResponse.success(providerChannelHttpConverter.toResponse(channel));
    }

    @PostMapping("/model-fetch-preview")
    public ApiResponse<ProviderModelPreviewResponse> previewProviderModels(
            @Valid @RequestBody AdminFetchProviderModelPreviewRequest fetchRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        FetchProviderModelPreviewCommand command = providerChannelHttpConverter.toFetchModelPreviewCommand(
                fetchRequest,
                operatorUserId
        );
        return ApiResponse.success(providerChannelHttpConverter.toPreviewResponse(
                providerChannelApplicationService.previewProviderModels(command)
        ));
    }

    @PatchMapping("/{provider-channel-id}/disable")
    public ApiResponse<ProviderChannelResponse> disableChannel(
            @PathVariable("provider-channel-id") Long providerChannelId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ChangeProviderChannelStatusCommand command = providerChannelHttpConverter.toChangeStatusCommand(
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        ProviderChannel channel = providerChannelApplicationService.disableChannel(command);
        return ApiResponse.success(providerChannelHttpConverter.toResponse(channel));
    }

    @PostMapping("/{provider-channel-id}/model-fetches")
    public ApiResponse<ProviderChannelResponse> fetchProviderModels(
            @PathVariable("provider-channel-id") Long providerChannelId,
            @Valid @RequestBody AdminFetchProviderModelsRequest fetchRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        FetchProviderModelsCommand command = providerChannelHttpConverter.toFetchModelsCommand(
                fetchRequest,
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        ProviderChannel channel = providerChannelApplicationService.fetchAndReplaceModels(command);
        return ApiResponse.success(providerChannelHttpConverter.toResponse(channel));
    }

    @PutMapping("/{provider-channel-id}/models/{channel-model-support-id}")
    public ApiResponse<ProviderChannelResponse> upsertChannelModel(
            @PathVariable("provider-channel-id") Long providerChannelId,
            @PathVariable("channel-model-support-id") Long channelModelSupportId,
            @Valid @RequestBody AdminUpsertChannelModelRequest upsertRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UpsertChannelModelCommand command = providerChannelHttpConverter.toUpsertModelCommand(
                upsertRequest,
                operatorUserId,
                ProviderChannelId.of(providerChannelId),
                ChannelModelSupportId.of(channelModelSupportId)
        );
        ProviderChannel channel = providerChannelApplicationService.upsertChannelModel(command);
        return ApiResponse.success(providerChannelHttpConverter.toResponse(channel));
    }

    @DeleteMapping("/{provider-channel-id}/models")
    public ApiResponse<ProviderChannelResponse> removeChannelModel(
            @PathVariable("provider-channel-id") Long providerChannelId,
            @RequestParam("requestedModel") String requestedModel,
            @RequestParam("upstreamProtocol") String upstreamProtocol,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        RemoveChannelModelCommand command = providerChannelHttpConverter.toRemoveModelCommand(
                requestedModel,
                upstreamProtocol,
                operatorUserId,
                ProviderChannelId.of(providerChannelId)
        );
        ProviderChannel channel = providerChannelApplicationService.removeChannelModel(command);
        return ApiResponse.success(providerChannelHttpConverter.toResponse(channel));
    }
}
