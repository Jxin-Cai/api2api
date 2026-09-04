package com.api2api.ohs.http.credential.converter;

import com.api2api.application.credential.ModelDailyLimitWindow;
import com.api2api.application.credential.command.CreateModelGroupCommand;
import com.api2api.application.credential.command.UpdateModelGroupCommand;
import com.api2api.application.credential.dto.ModelGroupView;
import com.api2api.domain.credential.model.ModelDailyLimits;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.credential.dto.ModelGroupListResponse;
import com.api2api.ohs.http.credential.dto.ModelGroupRequest;
import com.api2api.ohs.http.credential.dto.ModelGroupResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelGroupHttpConverter {

    @NonNull private final ModelDailyLimitWindow dailyLimitWindow;

    public CreateModelGroupCommand toCreateCommand(ModelGroupRequest request, UserAccountId ownerUserId,
                                                    ModelGroupId modelGroupId) {
        return CreateModelGroupCommand.builder()
                .ownerUserId(ownerUserId)
                .modelGroupId(modelGroupId)
                .name(ModelGroupName.of(request.getName()))
                .modelWhitelist(toWhitelist(request.getModelWhitelist()))
                .modelDailyLimits(toDailyLimits(request.getModelDailyLimits()))
                .build();
    }

    public UpdateModelGroupCommand toUpdateCommand(ModelGroupRequest request, UserAccountId ownerUserId,
                                                    ModelGroupId modelGroupId) {
        return UpdateModelGroupCommand.builder()
                .ownerUserId(ownerUserId)
                .modelGroupId(modelGroupId)
                .name(ModelGroupName.of(request.getName()))
                .modelWhitelist(toWhitelist(request.getModelWhitelist()))
                .modelDailyLimits(toDailyLimits(request.getModelDailyLimits()))
                .build();
    }

    public ModelGroupResponse toResponse(ModelGroupView view) {
        ModelGroup group = view.group();
        Map<String, BigDecimal> usage = new TreeMap<>();
        group.getModelDailyLimits().limits().keySet().forEach(model -> usage.put(
                model.getValue(), view.todayUsageByModel().getOrDefault(model, BigDecimal.ZERO)));
        return ModelGroupResponse.builder()
                .id(group.getId().value())
                .name(group.getName().getValue())
                .modelWhitelist(group.getModelWhitelist().getModels().stream().map(ModelName::getValue).sorted().toList())
                .modelDailyLimits(toDailyLimitsResponse(group.getModelDailyLimits()))
                .modelDailyUsage(usage)
                .rateLimitedModels(view.rateLimitedModels().stream().map(ModelName::getValue).sorted().toList())
                .dailyLimitZoneId(dailyLimitWindow.zone().getId())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public ModelGroupListResponse toListResponse(List<ModelGroupView> views) {
        return ModelGroupListResponse.builder().groups(views.stream().map(this::toResponse).toList()).build();
    }

    private ModelWhitelist toWhitelist(List<String> modelNames) {
        Set<ModelName> models = new LinkedHashSet<>();
        for (String modelName : modelNames) {
            models.add(ModelName.of(modelName));
        }
        return ModelWhitelist.of(models);
    }

    private ModelDailyLimits toDailyLimits(Map<String, Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return ModelDailyLimits.empty();
        }
        Map<ModelName, Long> limits = new LinkedHashMap<>();
        raw.forEach((model, limit) -> {
            if (limit != null && limit > 0) {
                limits.put(ModelName.of(model), limit);
            }
        });
        return ModelDailyLimits.of(limits);
    }

    private Map<String, Long> toDailyLimitsResponse(ModelDailyLimits limits) {
        Map<String, Long> response = new TreeMap<>();
        limits.limits().forEach((model, limit) -> response.put(model.getValue(), limit));
        return response;
    }
}
