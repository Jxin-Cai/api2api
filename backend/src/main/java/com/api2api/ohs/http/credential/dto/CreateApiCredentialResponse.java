package com.api2api.ohs.http.credential.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for creating an API credential, including one-time plaintext key material.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiCredentialResponse {

    private ApiCredentialResponse credential;
    private String plaintextApiKey;
}
