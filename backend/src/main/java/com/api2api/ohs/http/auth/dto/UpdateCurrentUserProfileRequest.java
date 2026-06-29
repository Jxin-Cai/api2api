package com.api2api.ohs.http.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Current user profile update request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCurrentUserProfileRequest {

    @NotBlank(message = "Display name must not be blank")
    private String displayName;
}
