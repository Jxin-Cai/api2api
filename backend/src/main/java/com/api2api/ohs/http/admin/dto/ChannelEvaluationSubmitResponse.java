package com.api2api.ohs.http.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Submitted channel evaluation runs response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEvaluationSubmitResponse {

    private List<ChannelEvaluationResponse> evaluations;
}
