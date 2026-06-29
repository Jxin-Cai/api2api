package com.api2api.ohs.http.dashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for admin dashboard query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAdminDashboardRequest {

    private String zoneId;

    @Min(1)
    private Integer recentRateMinutes;

    @Min(1)
    @Max(30)
    private Integer trendDays;
}
