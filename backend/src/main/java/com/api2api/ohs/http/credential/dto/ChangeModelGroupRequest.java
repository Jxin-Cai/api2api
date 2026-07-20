package com.api2api.ohs.http.credential.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeModelGroupRequest {
    @NotNull(message = "Model group id must not be null")
    private Long modelGroupId;
}
