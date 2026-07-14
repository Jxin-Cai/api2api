package com.api2api.ohs.http;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Factory for identifiers created at the HTTP boundary.
 */
@Component
public class IdentifierFactory {

    public UserAccountId newUserAccountId() {
        long timestampPart = System.currentTimeMillis() * 1_000L;
        long randomPart = ThreadLocalRandom.current().nextLong(1_000L);
        return UserAccountId.of(timestampPart + randomPart);
    }

    public ProviderChannelId newProviderChannelId() {
        long timestampPart = System.currentTimeMillis() * 1_000L;
        long randomPart = ThreadLocalRandom.current().nextLong(1_000L);
        return ProviderChannelId.of(timestampPart + randomPart);
    }

    public ApiCredentialId newApiCredentialId() {
        long timestampPart = System.currentTimeMillis() * 1_000L;
        long randomPart = ThreadLocalRandom.current().nextLong(1_000L);
        return ApiCredentialId.of(timestampPart + randomPart);
    }
}
