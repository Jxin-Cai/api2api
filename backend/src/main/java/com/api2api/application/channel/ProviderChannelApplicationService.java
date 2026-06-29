package com.api2api.application.channel;

import com.api2api.application.BusinessException;
import com.api2api.application.channel.command.ChangeProviderChannelStatusCommand;
import com.api2api.application.channel.command.CreateProviderChannelCommand;
import com.api2api.application.channel.command.FetchProviderModelsCommand;
import com.api2api.application.channel.command.RemoveChannelModelCommand;
import com.api2api.application.channel.command.UpdateProviderChannelCommand;
import com.api2api.application.channel.command.UpsertChannelModelCommand;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderChannelApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final ProviderChannelRepository providerChannelRepository;

    @NonNull
    private final ProviderModelFetchPort providerModelFetchPort;

    @NonNull
    private final Clock clock;

    @Transactional(rollbackFor = Exception.class)
    public ProviderChannel createChannel(CreateProviderChannelCommand command) {
        assertAdmin(command.getOperatorUserId());

        ProviderChannel channel = ProviderChannel.create(
                command.getProviderChannelId(),
                command.getName(),
                command.getHost(),
                command.getKeyRef(),
                command.getSupportedProtocols(),
                now()
        );
        providerChannelRepository.save(channel);
        return channel;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderChannel updateChannel(UpdateProviderChannelCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());
        Instant now = now();

        channel.rename(command.getName(), now);
        channel.updateEndpoint(command.getHost(), command.getKeyRef(), now);
        channel.replaceSupportedProtocols(command.getSupportedProtocols(), now);
        providerChannelRepository.save(channel);
        return channel;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderChannel enableChannel(ChangeProviderChannelStatusCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());

        channel.enable(now());
        providerChannelRepository.save(channel);
        return channel;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderChannel disableChannel(ChangeProviderChannelStatusCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());

        channel.disable(now());
        providerChannelRepository.save(channel);
        return channel;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ProviderChannel> listChannels(UserAccountId operatorUserId) {
        assertAdmin(operatorUserId);
        return providerChannelRepository.findAll();
    }

    public ProviderChannel fetchAndReplaceModels(FetchProviderModelsCommand command) {
        ProviderChannel channel = loadChannelForModelFetch(command);
        List<ChannelModelSupport> fetchedModels = providerModelFetchPort.fetchModels(
                channel.id(),
                channel.host(),
                channel.keyRef(),
                channel.supportedProtocols(),
                command.getDefaultPriority()
        );
        return replaceFetchedModels(command.getOperatorUserId(), command.getProviderChannelId(), fetchedModels);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProviderChannel loadChannelForModelFetch(FetchProviderModelsCommand command) {
        assertAdmin(command.getOperatorUserId());
        return loadChannel(command.getProviderChannelId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderChannel replaceFetchedModels(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId,
            List<ChannelModelSupport> fetchedModels
    ) {
        assertAdmin(operatorUserId);
        ProviderChannel channel = loadChannel(providerChannelId);

        if (fetchedModels == null || fetchedModels.isEmpty()) {
            throw new BusinessException("PROVIDER_MODELS_EMPTY");
        }
        channel.replaceModels(fetchedModels, now());
        providerChannelRepository.save(channel);
        return channel;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderChannel upsertChannelModel(UpsertChannelModelCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());
        Instant now = now();
        ChannelModelSupport modelSupport = ChannelModelSupport.create(
                command.getChannelModelSupportId(),
                command.getRequestedModel(),
                command.getUpstreamModel(),
                command.getUpstreamProtocol(),
                command.getPriority(),
                command.getSource(),
                now
        );

        channel.addOrUpdateModel(modelSupport, now);
        providerChannelRepository.save(channel);
        return channel;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderChannel removeChannelModel(RemoveChannelModelCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());

        channel.removeModel(command.getRequestedModel(), command.getUpstreamProtocol(), now());
        providerChannelRepository.save(channel);
        return channel;
    }

    private void assertAdmin(UserAccountId operatorUserId) {
        UserAccount operator = userAccountRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }

    private ProviderChannel loadChannel(ProviderChannelId providerChannelId) {
        return providerChannelRepository.findById(providerChannelId)
                .orElseThrow(() -> new BusinessException("PROVIDER_CHANNEL_NOT_FOUND"));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
