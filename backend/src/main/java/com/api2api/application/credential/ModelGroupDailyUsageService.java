package com.api2api.application.credential;

import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.usage.model.ModelGroupModelUsage;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.UserAccountId;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Loads today's per-model consumption for every model group of a user in one query, so both the
 * credential list and the group list can render daily-limit status without N+1 aggregation.
 */
@Service
@RequiredArgsConstructor
public class ModelGroupDailyUsageService {

    @NonNull private final UsageRecordRepository usageRecordRepository;
    @NonNull private final ModelDailyLimitWindow dailyLimitWindow;

    /** Returns {@code groupId -> (model -> weighted tokens consumed today)}; groups without usage are absent. */
    public Map<ModelGroupId, Map<ModelName, BigDecimal>> loadTodayUsageByGroup(UserAccountId ownerUserId) {
        Objects.requireNonNull(ownerUserId, "Owner user id must not be null");
        Map<ModelGroupId, Map<ModelName, BigDecimal>> usageByGroup = new HashMap<>();
        for (ModelGroupModelUsage usage : usageRecordRepository.sumActualTokensByOwnerGroupedByModel(
                ownerUserId, dailyLimitWindow.today())) {
            usageByGroup
                    .computeIfAbsent(usage.modelGroupId(), ignored -> new HashMap<>())
                    .merge(usage.model(), usage.actualTokens(), BigDecimal::add);
        }
        return Collections.unmodifiableMap(usageByGroup);
    }
}
