package com.api2api.ohs.http.admin.dto;

import com.api2api.domain.user.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Password must not be blank")
    @Size(max = 128, message = "Password length must not exceed 128 characters")
    private String password;

    @NotNull(message = "Role must not be null")
    private UserRole role;
}
