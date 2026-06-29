package com.api2api.ohs.http.credential.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for renaming an API credential.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RenameApiCredentialRequest {

    @NotBlank(message = "API credential name must not be blank")
    private String name;
}
