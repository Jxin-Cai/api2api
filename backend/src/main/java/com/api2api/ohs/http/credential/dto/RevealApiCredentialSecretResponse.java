package com.api2api.ohs.http.credential.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevealApiCredentialSecretResponse {

    private Long apiCredentialId;
    private String keyPreview;
    private String plaintextApiKey;
}
