package com.api2api.ohs.http.dashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for front per-credential dashboard metrics query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetFrontKeyMetricsRequest {

    private String zoneId;

    @Min(1)
    @Max(30)
    private Integer trendDays;

    private List<Long> credentialIds;
}
