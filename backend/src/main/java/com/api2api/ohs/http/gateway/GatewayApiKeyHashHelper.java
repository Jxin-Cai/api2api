package com.api2api.ohs.http.gateway;

import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.ohs.http.ApiKeyHasher;
import org.springframework.stereotype.Component;

/**
 * Converts a Bearer API key into the domain hash representation without exposing plaintext key material.
 */
@Component
public class GatewayApiKeyHashHelper {

    private static final String BEARER_PREFIX = "Bearer ";

    public ApiKeyHash hashGatewayApiKey(String authorizationHeader, String apiKeyHeader) {
        String apiKey = extractGatewayApiKey(authorizationHeader, apiKeyHeader);
        return ApiKeyHasher.hash(apiKey);
    }

    public ApiKeyHash hashBearerToken(String authorizationHeader) {
        return hashGatewayApiKey(authorizationHeader, null);
    }

    private String extractGatewayApiKey(String authorizationHeader, String apiKeyHeader) {
        String bearerToken = extractBearerTokenOrNull(authorizationHeader);
        if (bearerToken != null) {
            return bearerToken;
        }
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return apiKeyHeader.trim();
        }
        throw new IllegalArgumentException("Authorization bearer token or x-api-key is required");
    }

    private String extractBearerTokenOrNull(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String trimmed = authorizationHeader.trim();
        if (!trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new IllegalArgumentException("Authorization bearer token is invalid");
        }
        String token = trimmed.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Authorization bearer token is invalid");
        }
        return token;
    }

}
