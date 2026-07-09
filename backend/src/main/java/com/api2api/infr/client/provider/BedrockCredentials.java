package com.api2api.infr.client.provider;

import java.util.Objects;

record BedrockCredentials(String accessKeyId, String secretAccessKey, String sessionToken, String region) {

    BedrockCredentials {
        Objects.requireNonNull(accessKeyId, "Access key ID must not be null");
        Objects.requireNonNull(secretAccessKey, "Secret access key must not be null");
        Objects.requireNonNull(region, "Region must not be null");
    }

    BedrockCredentials(String accessKeyId, String secretAccessKey, String region) {
        this(accessKeyId, secretAccessKey, null, region);
    }
}
