package com.api2api.ohs.http.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for previewing provider models from a saved channel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFetchProviderChannelModelPreviewRequest {

    @NotNull(message = "Default priority must not be null")
    @Min(value = 1, message = "Default priority must be greater than or equal to 1")
    private Integer defaultPriority;
}
