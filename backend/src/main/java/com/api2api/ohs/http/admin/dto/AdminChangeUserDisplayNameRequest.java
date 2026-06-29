package com.api2api.ohs.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin change user display name request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminChangeUserDisplayNameRequest {

    @NotBlank(message = "Display name must not be blank")
    private String displayName;
}
