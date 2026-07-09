package com.api2api.infr.client.provider;

import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class BedrockCredentialResolver {

    private static final Pattern REGION_PATTERN = Pattern.compile("bedrock-runtime\\.([a-z0-9-]+)\\.amazonaws\\.com");

    private final ProviderSecretResolver secretResolver;
    private final Environment environment;

    BedrockCredentialResolver(ProviderSecretResolver secretResolver, Environment environment) {
        this.secretResolver = secretResolver;
        this.environment = environment;
    }

    BedrockCredentials resolve(ProviderHost host, ProviderKeyRef keyRef) {
        String region = extractRegion(host);
        String resolvedSecret = secretResolver.resolve(keyRef);

        if (resolvedSecret.contains(":")) {
            String[] parts = resolvedSecret.split(":", 3);
            String accessKeyId = parts[0];
            String secretAccessKey = parts[1];
            String sessionToken = parts.length > 2 ? parts[2] : null;
            return new BedrockCredentials(accessKeyId, secretAccessKey, sessionToken, region);
        }

        String accessKeyId = resolveEnvVar("AWS_ACCESS_KEY_ID");
        String secretAccessKey = resolveEnvVar("AWS_SECRET_ACCESS_KEY");
        String sessionToken = resolveEnvVar("AWS_SESSION_TOKEN");
        if (accessKeyId != null && secretAccessKey != null) {
            return new BedrockCredentials(accessKeyId, secretAccessKey, sessionToken, region);
        }

        throw new ProviderSecretResolveException("Cannot resolve AWS credentials from keyRef: " + keyRef);
    }

    String extractRegion(ProviderHost host) {
        Matcher matcher = REGION_PATTERN.matcher(host.value());
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Cannot extract AWS region from host: " + host.value());
    }

    private String resolveEnvVar(String name) {
        String value = environment.getProperty(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : null;
    }
}
