package com.api2api.ohs.http.credential.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for listing API credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCredentialListResponse {

    private List<ApiCredentialResponse> credentials;
}
