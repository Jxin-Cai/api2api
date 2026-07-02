package com.api2api.ohs.http.credential.dto;

import com.api2api.domain.credential.model.ApiCredentialStatus;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API credential response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCredentialResponse {

    private Long id;
    private Long ownerUserId;
    private String name;
    private String keyPreview;
    private List<String> modelWhitelist;
    private long tokenLimit;
    private long consumedTokens;
    private Long remainingTokens;
    private ApiCredentialStatus status;
    private Instant lastUsedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
