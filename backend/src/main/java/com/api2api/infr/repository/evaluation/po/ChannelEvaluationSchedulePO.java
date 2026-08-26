package com.api2api.infr.repository.evaluation.po;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEvaluationSchedulePO {

    private Long id;
    private Long providerChannelId;
    private String cronExpression;
    private String zoneId;
    private String models;
    private boolean enabled;
    private Instant lastTriggeredAt;
    private Instant nextTriggerAt;
    private Instant createdAt;
    private Instant updatedAt;
}
