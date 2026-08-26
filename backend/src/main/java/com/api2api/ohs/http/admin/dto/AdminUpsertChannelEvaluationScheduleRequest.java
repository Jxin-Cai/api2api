package com.api2api.ohs.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for creating or replacing a channel evaluation schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpsertChannelEvaluationScheduleRequest {

    @NotBlank(message = "Cron expression must not be blank")
    private String cronExpression;

    private String zoneId;

    @Size(max = 200)
    private List<String> models;

    private Boolean enabled;
}
