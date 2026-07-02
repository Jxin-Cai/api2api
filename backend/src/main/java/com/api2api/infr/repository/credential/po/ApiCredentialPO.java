package com.api2api.infr.repository.credential.po;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persistence object mapped to the api_credentials table.
 * Plaintext API key material is intentionally absent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCredentialPO {

    private Long id;
    private Long ownerUserId;
    private String name;
    private String keyHash;
    private String keyPreview;
    private String encryptedKeyMaterial;
    private String keyMaterialNonce;
    private int keyMaterialVersion;
    private String modelWhitelist;
    private long tokenLimit;
    private String status;
    private Instant lastUsedTime;
    private Instant createdTime;
    private Instant updatedTime;
    private boolean deleted;
}
