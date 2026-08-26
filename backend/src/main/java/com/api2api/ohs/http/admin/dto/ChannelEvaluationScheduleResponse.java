package com.api2api.ohs.http.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Channel evaluation schedule response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEvaluationScheduleResponse {

    private Long id;
    private Long providerChannelId;
    private String cronExpression;
    private String zoneId;
    private List<String> models;
    private boolean enabled;
    private Long lastTriggeredAt;
    private Long nextTriggerAt;
    private Long createdAt;
    private Long updatedAt;
}
