package com.api2api.ohs.http.admin.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for submitting channel evaluation runs.
 *
 * <p>An empty {@code models} list means every currently enabled model of the channel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubmitChannelEvaluationRequest {

    @Size(max = 200)
    private List<String> models;
}
