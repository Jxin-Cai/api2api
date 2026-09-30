package com.api2api.application.channel;

import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.credential.model.ModelName;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Current provider support, derived without modifying model permissions or explicit switches. */
@Service
@RequiredArgsConstructor
public class ProviderModelAvailabilityService {

    @NonNull private final ProviderChannelRepository channelRepository;
    @NonNull private final Clock clock;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Set<ModelName> availableModels() {
        Instant now = Instant.now(clock);
        // Read current configuration on every request; do not cache availability across channel changes.
        return channelRepository.findAll().stream()
                .filter(ProviderChannel::isEnabledForRouting)
                .flatMap(channel -> channel.supportedModels().stream()
                        .filter(model -> channel.supportsUpstreamProtocol(model.upstreamProtocol())))
                .filter(model -> isAvailable(model, now))
                .map(model -> ModelName.of(model.requestedModel().value()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isAvailable(ChannelModelSupport model, Instant now) {
        // Routing restores expired isolation before selecting a channel. Model listing must also
        // recover at that instant, without a database write inside this read-only query.
        return model.status() == ChannelModelStatus.ENABLED
                || (model.status() == ChannelModelStatus.RATE_LIMITED
                    && model.rateLimitResetAt() != null
                    && !model.rateLimitResetAt().isAfter(now));
    }
}
