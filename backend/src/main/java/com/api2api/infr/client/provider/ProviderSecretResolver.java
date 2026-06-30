package com.api2api.infr.client.provider;

import com.api2api.domain.channel.model.ProviderKeyRef;
import java.util.Arrays;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Resolves provider key references from secure configuration sources.
 */
@Component
@RequiredArgsConstructor
public class ProviderSecretResolver {

    @NonNull
    private final ProviderSecretProperties properties;

    @NonNull
    private final Environment environment;

    public String resolve(ProviderKeyRef keyRef) {
        Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        String ref = keyRef.value();

        String profileValue = resolveFromActiveProfiles(ref);
        if (hasText(profileValue)) {
            return profileValue;
        }

        String configuredValue = properties.findByRef(null, ref);
        if (hasText(configuredValue)) {
            return configuredValue;
        }

        String environmentValue = environment.getProperty(ref);
        if (hasText(environmentValue)) {
            return environmentValue;
        }

        String systemEnvironmentValue = System.getenv(ref);
        if (hasText(systemEnvironmentValue)) {
            return systemEnvironmentValue;
        }

        return ref;
    }

    private String resolveFromActiveProfiles(String ref) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> properties.findByRef(profile, ref))
                .filter(ProviderSecretResolver::hasText)
                .findFirst()
                .orElse(null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
