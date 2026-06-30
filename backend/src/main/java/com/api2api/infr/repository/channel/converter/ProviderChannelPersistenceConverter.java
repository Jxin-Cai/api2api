package com.api2api.infr.repository.channel.converter;

import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderChannelStatus;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.infr.repository.channel.po.ChannelModelSupportPO;
import com.api2api.infr.repository.channel.po.ProviderChannelPO;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts provider channel aggregates to persistence objects.
 */
@Mapper(config = MapStructConfig.class)
public interface ProviderChannelPersistenceConverter {

    @Mapping(target = "id", expression = "java(providerChannel.id().value())")
    @Mapping(target = "name", expression = "java(providerChannel.name().value())")
    @Mapping(target = "host", expression = "java(providerChannel.host().value())")
    @Mapping(target = "keyRef", expression = "java(providerChannel.keyRef().value())")
    @Mapping(target = "routePriority", expression = "java(providerChannel.routePriority())")
    @Mapping(target = "supportedProtocols", expression = "java(toProtocolText(providerChannel.supportedProtocols()))")
    @Mapping(target = "status", expression = "java(providerChannel.status().name())")
    @Mapping(target = "createdTime", expression = "java(providerChannel.createdAt())")
    @Mapping(target = "updatedTime", expression = "java(providerChannel.updatedAt())")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "supportedModels", expression = "java(toModelPOs(providerChannel.id().value(), providerChannel.supportedModels()))")
    ProviderChannelPO toPO(ProviderChannel providerChannel);

    default ProviderChannel toDomain(ProviderChannelPO po) {
        return ProviderChannel.rehydrate(
                ProviderChannelId.of(po.getId()),
                ProviderChannelName.of(po.getName()),
                ProviderHost.of(po.getHost()),
                ProviderKeyRef.of(po.getKeyRef()),
                po.getRoutePriority(),
                toProtocols(po.getSupportedProtocols()),
                po.getSupportedModels().stream().map(this::toModelDomain).toList(),
                ProviderChannelStatus.valueOf(po.getStatus()),
                po.getCreatedTime(),
                po.getUpdatedTime()
        );
    }

    default List<ChannelModelSupportPO> toModelPOs(Long providerChannelId, List<ChannelModelSupport> models) {
        return models.stream()
                .map(model -> toModelPO(providerChannelId, model))
                .toList();
    }

    @Mapping(target = "id", expression = "java(model.id().value())")
    @Mapping(target = "providerChannelId", expression = "java(providerChannelId)")
    @Mapping(target = "requestedModel", expression = "java(model.requestedModel().value())")
    @Mapping(target = "upstreamModel", expression = "java(model.upstreamModel().value())")
    @Mapping(target = "upstreamProtocol", expression = "java(model.upstreamProtocol().name())")
    @Mapping(target = "priority", expression = "java(model.priority().value())")
    @Mapping(target = "preferred", expression = "java(model.preferred())")
    @Mapping(target = "source", expression = "java(model.source().name())")
    @Mapping(target = "status", expression = "java(model.status().name())")
    @Mapping(target = "createdTime", expression = "java(model.createdAt())")
    @Mapping(target = "updatedTime", expression = "java(model.updatedAt())")
    ChannelModelSupportPO toModelPO(Long providerChannelId, ChannelModelSupport model);

    default ChannelModelSupport toModelDomain(ChannelModelSupportPO po) {
        return ChannelModelSupport.rehydrate(
                ChannelModelSupportId.of(po.getId()),
                ModelName.of(po.getRequestedModel()),
                ModelName.of(po.getUpstreamModel()),
                ProtocolType.valueOf(po.getUpstreamProtocol()),
                RoutePriority.of(po.getPriority()),
                po.isPreferred(),
                ChannelModelStatus.valueOf(po.getStatus()),
                ModelSupportSource.valueOf(po.getSource()),
                po.getCreatedTime(),
                po.getUpdatedTime()
        );
    }

    default String toProtocolText(Set<ProtocolType> protocols) {
        return protocols.stream().map(ProtocolType::name).collect(Collectors.joining(","));
    }

    default Set<ProtocolType> toProtocols(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(ProtocolType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
