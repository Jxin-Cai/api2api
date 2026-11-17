package com.api2api.application.channel;

import com.api2api.application.BusinessException;
import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
    @NonNull private final ModelGroupRepository groups;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Set<ModelName> availableModels() {
        return availableModels(channelRepository.findAll());
    }

    public Set<ModelName> availableModels(ModelGroup group) {
        return availableModels(channelRepository.findAll().stream().filter(channel -> group.allowsChannel(channel.id())).toList());
    }

    public Set<ModelName> availableModels(ModelGroupId groupId) {
        return availableModels(allowedChannels(groupId, channelRepository.findAll()));
    }

    public List<ProviderChannel> allowedChannels(ModelGroupId groupId, List<ProviderChannel> channels) {
        // Fail closed when the group has disappeared; never fall back to global routes.
        return groups.findById(groupId)
                .map(group -> channels.stream().filter(channel -> group.allowsChannel(channel.id())).toList())
                .orElse(List.of());
    }

    public void assertChannelsExist(Set<ProviderChannelId> channelIds) {
        Set<ProviderChannelId> existing = channelRepository.findAll().stream().map(ProviderChannel::id).collect(Collectors.toSet());
        if (!existing.containsAll(channelIds)) {
            throw new BusinessException("PROVIDER_CHANNEL_NOT_FOUND", "所选渠道不存在或已删除，请刷新后重试");
        }
    }

    private Set<ModelName> availableModels(List<ProviderChannel> channels) {
        Instant now = Instant.now(clock);
        return channels.stream()
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
