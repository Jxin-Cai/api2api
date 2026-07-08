package com.api2api.application.channel;

import com.api2api.application.BusinessException;
import com.api2api.application.channel.command.BatchUpsertChannelModelsCommand;
import com.api2api.application.channel.command.ChangeProviderChannelStatusCommand;
import com.api2api.application.channel.command.ChannelModelUpsertItemCommand;
import com.api2api.application.channel.command.CreateProviderChannelCommand;
import com.api2api.application.channel.command.DeleteProviderChannelCommand;
import com.api2api.application.channel.command.FetchProviderChannelModelPreviewCommand;
import com.api2api.application.channel.command.FetchProviderModelPreviewCommand;
import com.api2api.application.channel.command.FetchProviderModelsCommand;
import com.api2api.application.channel.command.RemoveChannelModelCommand;
import com.api2api.application.channel.command.UpdateProviderChannelCommand;
import com.api2api.application.channel.command.UpsertChannelModelCommand;
import com.api2api.application.channel.dto.ProviderModelOption;
import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
                command.getModelsPath(),
                command.getRoutePriority(),
                command.getProtocolMappings(),
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
        channel.updateEndpoint(command.getHost(), command.getKeyRef() == null ? channel.keyRef() : command.getKeyRef(), now);
        channel.changeModelsPath(command.getModelsPath(), now);
        channel.changeRoutePriority(command.getRoutePriority(), now);
        channel.replaceProtocolMappings(command.getProtocolMappings(), now);
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

    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(DeleteProviderChannelCommand command) {
        assertAdmin(command.getOperatorUserId());
        loadChannel(command.getProviderChannelId());
        providerChannelRepository.softDeleteById(command.getProviderChannelId(), now());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ProviderChannel> listChannels(UserAccountId operatorUserId) {
        assertAdmin(operatorUserId);
        return providerChannelRepository.findAll();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ProviderModelOption> listProviderModelOptions(UserAccountId userId) {
        assertUserPortal(userId);
        Map<String, ModelOptionAccumulator> options = new LinkedHashMap<>();
        for (ProviderChannel channel : providerChannelRepository.findEnabledForRouting()) {
            for (ChannelModelSupport modelSupport : channel.supportedModels()) {
                if (modelSupport.status() != ChannelModelStatus.ENABLED) {
                    continue;
                }
                String modelName = modelSupport.requestedModel().value();
                options.computeIfAbsent(modelName, ignored -> new ModelOptionAccumulator(modelSupport.requestedModel()))
                        .add(channel, modelSupport.upstreamProtocol());
            }
        }
        return options.values().stream()
                .map(ModelOptionAccumulator::toOption)
                .sorted(Comparator.comparing(option -> option.model().value()))
                .toList();
    }

    public ProviderChannel fetchAndReplaceModels(FetchProviderModelsCommand command) {
        ProviderChannel channel = loadChannelForModelFetch(command);
        List<ChannelModelSupport> fetchedModels = providerModelFetchPort.fetchModels(
                channel.id(),
                channel.host(),
                channel.keyRef(),
                channel.modelsPath(),
                channel.upstreamProtocols(),
                command.getDefaultPriority()
        );
        return replaceFetchedModels(command.getOperatorUserId(), command.getProviderChannelId(), fetchedModels);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ChannelModelSupport> previewProviderModels(FetchProviderModelPreviewCommand command) {
        assertAdmin(command.getOperatorUserId());
        return providerModelFetchPort.fetchModels(
                ProviderChannelId.of(1L),
                command.getHost(),
                command.getKeyRef(),
                command.getModelsPath(),
                command.getUpstreamProtocols(),
                command.getDefaultPriority()
        );
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ChannelModelSupport> previewProviderModels(FetchProviderChannelModelPreviewCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());
        Set<ProtocolType> upstreamProtocols = command.getUpstreamProtocols() == null || command.getUpstreamProtocols().isEmpty()
                ? channel.upstreamProtocols()
                : command.getUpstreamProtocols();
        return providerModelFetchPort.fetchModels(
                channel.id(),
                command.getHost() == null ? channel.host() : command.getHost(),
                command.getKeyRef() == null ? channel.keyRef() : command.getKeyRef(),
                command.getModelsPath() == null ? channel.modelsPath() : command.getModelsPath(),
                upstreamProtocols,
                command.getDefaultPriority()
        );
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
    public ProviderChannel upsertChannelModels(BatchUpsertChannelModelsCommand command) {
        assertAdmin(command.getOperatorUserId());
        ProviderChannel channel = loadChannel(command.getProviderChannelId());
        Instant now = now();
        List<ChannelModelSupport> modelSupports = command.getModels().stream()
                .map(item -> toModelSupport(item, channel, now))
                .toList();
        if (command.isReplaceExisting()) {
            channel.replaceModels(modelSupports, now);
        } else {
            modelSupports.forEach(modelSupport -> channel.addOrUpdateModel(modelSupport, now));
        }
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
                command.isPreferred(),
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

    private ChannelModelSupport toModelSupport(ChannelModelUpsertItemCommand item, ProviderChannel channel, Instant now) {
        ChannelModelSupport existing = item.getChannelModelSupportId() == null
                ? channel.findModelSupport(item.getRequestedModel(), item.getUpstreamProtocol()).orElse(null)
                : channel.supportedModels().stream()
                .filter(modelSupport -> modelSupport.id().equals(item.getChannelModelSupportId()))
                .findFirst()
                .orElse(null);
        return ChannelModelSupport.create(
                existing == null ? (item.getChannelModelSupportId() == null ? nextChannelModelSupportId() : item.getChannelModelSupportId()) : existing.id(),
                item.getRequestedModel(),
                item.getUpstreamModel(),
                item.getUpstreamProtocol(),
                item.getPriority(),
                item.isPreferred(),
                item.getSource(),
                now
        );
    }

    private void assertAdmin(UserAccountId operatorUserId) {
        UserAccount operator = userAccountRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }

    private void assertUserPortal(UserAccountId userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        user.assertCanAccess(AccessScope.USER_PORTAL);
    }

    private ProviderChannel loadChannel(ProviderChannelId providerChannelId) {
        return providerChannelRepository.findById(providerChannelId)
                .orElseThrow(() -> new BusinessException("PROVIDER_CHANNEL_NOT_FOUND"));
    }

    private ChannelModelSupportId nextChannelModelSupportId() {
        long timestampPart = System.currentTimeMillis() * 1_000L;
        long randomPart = ThreadLocalRandom.current().nextLong(1_000L);
        return ChannelModelSupportId.of(timestampPart + randomPart);
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private static final class ModelOptionAccumulator {
        private final com.api2api.domain.channel.model.ModelName model;
        private final Set<ProviderChannelId> channelIds = new LinkedHashSet<>();
        private final Set<ProtocolType> protocols = new LinkedHashSet<>();

        private ModelOptionAccumulator(com.api2api.domain.channel.model.ModelName model) {
            this.model = model;
        }

        private ModelOptionAccumulator add(ProviderChannel channel, ProtocolType protocol) {
            channelIds.add(channel.id());
            protocols.add(protocol);
            return this;
        }

        private ProviderModelOption toOption() {
            return ProviderModelOption.of(model, channelIds.size(), protocols);
        }
    }
}
