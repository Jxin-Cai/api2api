package com.api2api.ohs.http.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for batch upserting channel model support items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBatchUpsertChannelModelsRequest {

    private Boolean replaceExisting;

    @Valid
    @NotEmpty(message = "Models must not be empty")
    private List<AdminBatchUpsertChannelModelItemRequest> models;
}
