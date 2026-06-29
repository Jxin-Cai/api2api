package com.api2api.ohs.http.credential.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for changing an API credential token limit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTokenLimitRequest {

    @NotNull(message = "Token limit must not be null")
    @Min(value = 0, message = "Token limit must not be negative")
    private Long tokenLimit;
}
