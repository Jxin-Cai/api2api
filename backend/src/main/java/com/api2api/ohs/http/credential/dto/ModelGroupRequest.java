package com.api2api.ohs.http.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelGroupRequest {
    @NotBlank(message = "Model group name must not be blank")
    private String name;

    @NotNull(message = "Model whitelist must not be null")
    private List<String> modelWhitelist;
}
