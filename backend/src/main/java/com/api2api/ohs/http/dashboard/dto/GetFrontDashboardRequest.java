package com.api2api.ohs.http.dashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for front dashboard query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetFrontDashboardRequest {

    private String zoneId;

    @Min(1)
    private Integer recentCallsPage;

    @Min(20)
    @Max(200)
    private Integer recentCallsSize;
}
