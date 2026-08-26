package com.api2api.ohs.http.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query request for channel evaluation history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryChannelEvaluationHistoryRequest {

    private String requestedModel;
    private String status;
    private Instant from;
    private Instant to;
    private String sortField;
    private Boolean descending;

    @Min(1)
    @Max(500)
    private Integer limit;

    @Min(0)
    private Integer offset;
}
