package com.api2api.ohs.http.credential.converter;

import com.api2api.application.credential.command.CreateModelGroupCommand;
import com.api2api.application.credential.command.UpdateModelGroupCommand;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.credential.dto.ModelGroupListResponse;
import com.api2api.ohs.http.credential.dto.ModelGroupRequest;
import com.api2api.ohs.http.credential.dto.ModelGroupResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ModelGroupHttpConverter {

    public CreateModelGroupCommand toCreateCommand(ModelGroupRequest request, UserAccountId ownerUserId,
                                                    ModelGroupId modelGroupId) {
        return CreateModelGroupCommand.builder()
                .ownerUserId(ownerUserId)
                .modelGroupId(modelGroupId)
                .name(ModelGroupName.of(request.getName()))
                .modelWhitelist(toWhitelist(request.getModelWhitelist()))
                .build();
    }

    public UpdateModelGroupCommand toUpdateCommand(ModelGroupRequest request, UserAccountId ownerUserId,
                                                    ModelGroupId modelGroupId) {
        return UpdateModelGroupCommand.builder()
                .ownerUserId(ownerUserId)
                .modelGroupId(modelGroupId)
                .name(ModelGroupName.of(request.getName()))
                .modelWhitelist(toWhitelist(request.getModelWhitelist()))
                .build();
    }

    public ModelGroupResponse toResponse(ModelGroup group) {
        return ModelGroupResponse.builder()
                .id(group.getId().value())
                .name(group.getName().getValue())
                .modelWhitelist(group.getModelWhitelist().getModels().stream().map(ModelName::getValue).sorted().toList())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public ModelGroupListResponse toListResponse(List<ModelGroup> groups) {
        return ModelGroupListResponse.builder().groups(groups.stream().map(this::toResponse).toList()).build();
    }

    private ModelWhitelist toWhitelist(List<String> modelNames) {
        Set<ModelName> models = new LinkedHashSet<>();
        for (String modelName : modelNames) {
            models.add(ModelName.of(modelName));
        }
        return ModelWhitelist.of(models);
    }
}
