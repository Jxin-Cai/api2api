package com.api2api.ohs.http.admin.dto;

import com.api2api.domain.user.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin create user request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateUserRequest {

    @NotBlank(message = "Username must not be blank")
    private String username;

    @NotBlank(message = "Display name must not be blank")
    private String displayName;

    @NotNull(message = "Role must not be null")
    private UserRole role;
}
