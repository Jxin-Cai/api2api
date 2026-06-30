package com.api2api.ohs.http.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request item for batch upserting channel model support items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBatchUpsertChannelModelItemRequest {

    private Long id;

    @NotBlank(message = "Requested model must not be blank")
    private String requestedModel;

    @NotBlank(message = "Upstream model must not be blank")
    private String upstreamModel;

    @NotBlank(message = "Upstream protocol must not be blank")
    private String upstreamProtocol;

    @NotNull(message = "Priority must not be null")
    @Min(value = 1, message = "Priority must be greater than or equal to 1")
    private Integer priority;

    private Boolean preferred;

    @NotBlank(message = "Source must not be blank")
    private String source;
}
