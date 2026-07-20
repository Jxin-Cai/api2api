package com.api2api.ohs.http.credential.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for creating a new API credential.
 * Maps to CreateApiCredentialCommand after enriching with current user and generated key materials.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiCredentialRequest {

    @NotBlank(message = "API credential name must not be blank")
    private String name;

    @NotNull(message = "Model group id must not be null")
    private Long modelGroupId;

    @NotNull(message = "Token limit must not be null")
    @Min(value = 0, message = "Token limit must not be negative")
    private Long tokenLimit;
}
