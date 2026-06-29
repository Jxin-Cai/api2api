package com.api2api.ohs.http.credential.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for replacing an API credential model whitelist.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplaceModelWhitelistRequest {

    @NotNull(message = "Model whitelist must not be null")
    private List<String> modelWhitelist;
}
