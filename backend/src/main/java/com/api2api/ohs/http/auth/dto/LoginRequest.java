package com.api2api.ohs.http.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for MVP login by username.
 */
@Data
public class LoginRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may contain only letters, numbers, underscores, hyphens and dots")
    private String username;
}
