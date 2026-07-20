package com.api2api.ohs.http.credential;

import com.api2api.application.credential.ModelGroupApplicationService;
import com.api2api.application.credential.command.DeleteModelGroupCommand;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.IdentifierFactory;
import com.api2api.ohs.http.credential.converter.ModelGroupHttpConverter;
import com.api2api.ohs.http.credential.dto.ModelGroupListResponse;
import com.api2api.ohs.http.credential.dto.ModelGroupRequest;
import com.api2api.ohs.http.credential.dto.ModelGroupResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model-groups")
@RequiredArgsConstructor
public class ModelGroupController {

    @NonNull private final ModelGroupApplicationService applicationService;
    @NonNull private final ModelGroupHttpConverter converter;
    @NonNull private final CurrentUserContextResolver currentUserContextResolver;
    @NonNull private final IdentifierFactory identifierFactory;

    @GetMapping
    public ApiResponse<ModelGroupListResponse> listMyGroups(HttpServletRequest request) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        return ApiResponse.success(converter.toListResponse(applicationService.listMyGroups(ownerUserId)));
    }

    @PostMapping
    public ApiResponse<ModelGroupResponse> createGroup(@Valid @RequestBody ModelGroupRequest body,
                                                        HttpServletRequest request) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ModelGroup group = applicationService.createGroup(converter.toCreateCommand(
                body, ownerUserId, identifierFactory.newModelGroupId()));
        return ApiResponse.success(converter.toResponse(group));
    }

    @PutMapping("/{model-group-id}")
    public ApiResponse<ModelGroupResponse> updateGroup(@PathVariable("model-group-id") Long groupId,
                                                        @Valid @RequestBody ModelGroupRequest body,
                                                        HttpServletRequest request) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ModelGroup group = applicationService.updateGroup(converter.toUpdateCommand(
                body, ownerUserId, ModelGroupId.of(groupId)));
        return ApiResponse.success(converter.toResponse(group));
    }

    @DeleteMapping("/{model-group-id}")
    public ApiResponse<Void> deleteGroup(@PathVariable("model-group-id") Long groupId,
                                         HttpServletRequest request) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        applicationService.deleteGroup(DeleteModelGroupCommand.builder()
                .ownerUserId(ownerUserId)
                .modelGroupId(ModelGroupId.of(groupId))
                .build());
        return ApiResponse.success();
    }
}
