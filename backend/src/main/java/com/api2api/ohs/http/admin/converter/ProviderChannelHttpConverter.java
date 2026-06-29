package com.api2api.ohs.http.admin.converter;

import com.api2api.application.channel.command.ChangeProviderChannelStatusCommand;
import com.api2api.application.channel.command.CreateProviderChannelCommand;
import com.api2api.application.channel.command.FetchProviderModelsCommand;
import com.api2api.application.channel.command.RemoveChannelModelCommand;
import com.api2api.application.channel.command.UpdateProviderChannelCommand;
import com.api2api.application.channel.command.UpsertChannelModelCommand;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.admin.dto.AdminCreateProviderChannelRequest;
import com.api2api.ohs.http.admin.dto.AdminFetchProviderModelsRequest;
import com.api2api.ohs.http.admin.dto.AdminUpdateProviderChannelRequest;
import com.api2api.ohs.http.admin.dto.AdminUpsertChannelModelRequest;
import com.api2api.ohs.http.admin.dto.ChannelModelSupportResponse;
import com.api2api.ohs.http.admin.dto.ProviderChannelListResponse;
import com.api2api.ohs.http.admin.dto.ProviderChannelResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts provider channel HTTP models to application commands and responses.
 */
@Mapper(config = MapStructConfig.class)
public interface ProviderChannelHttpConverter {

    default CreateProviderChannelCommand toCreateCommand(
            AdminCreateProviderChannelRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        return CreateProviderChannelCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .name(ProviderChannelName.of(request.getName()))
                .host(ProviderHost.of(request.getHost()))
                .keyRef(ProviderKeyRef.of(request.getKeyRef()))
                .supportedProtocols(toProtocolTypes(request.getSupportedProtocols()))
                .build();
    }

    default UpdateProviderChannelCommand toUpdateCommand(
            AdminUpdateProviderChannelRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        return UpdateProviderChannelCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .name(ProviderChannelName.of(request.getName()))
                .host(ProviderHost.of(request.getHost()))
                .keyRef(ProviderKeyRef.of(request.getKeyRef()))
                .supportedProtocols(toProtocolTypes(request.getSupportedProtocols()))
                .build();
    }

    default ChangeProviderChannelStatusCommand toChangeStatusCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        return ChangeProviderChannelStatusCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .build();
    }

    default FetchProviderModelsCommand toFetchModelsCommand(
            AdminFetchProviderModelsRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        return FetchProviderModelsCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .defaultPriority(RoutePriority.of(request.getDefaultPriority()))
                .build();
    }

    default UpsertChannelModelCommand toUpsertModelCommand(
            AdminUpsertChannelModelRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId,
            ChannelModelSupportId channelModelSupportId
    ) {
        return UpsertChannelModelCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .channelModelSupportId(channelModelSupportId)
                .requestedModel(ModelName.of(request.getRequestedModel()))
                .upstreamModel(ModelName.of(request.getUpstreamModel()))
                .upstreamProtocol(toProtocolType(request.getUpstreamProtocol()))
                .priority(RoutePriority.of(request.getPriority()))
                .source(ModelSupportSource.valueOf(request.getSource()))
                .build();
    }

    default RemoveChannelModelCommand toRemoveModelCommand(
            String requestedModel,
            String upstreamProtocol,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        return RemoveChannelModelCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .requestedModel(ModelName.of(requestedModel))
                .upstreamProtocol(toProtocolType(upstreamProtocol))
                .build();
    }

    default ProviderChannelListResponse toListResponse(List<ProviderChannel> channels) {
        return ProviderChannelListResponse.builder()
                .channels(channels.stream().map(this::toResponse).toList())
                .build();
    }

    @Mapping(target = "id", expression = "java(channel.id().value())")
    @Mapping(target = "name", expression = "java(channel.name().value())")
    @Mapping(target = "host", expression = "java(channel.host().value())")
    @Mapping(target = "keyRef", expression = "java(channel.keyRef().value())")
    @Mapping(target = "supportedProtocols", expression = "java(toProtocolNames(channel.supportedProtocols()))")
    @Mapping(target = "supportedModels", expression = "java(toModelSupportResponses(channel.supportedModels()))")
    @Mapping(target = "status", expression = "java(channel.status().name())")
    @Mapping(target = "createdAt", expression = "java(channel.createdAt().toEpochMilli())")
    @Mapping(target = "updatedAt", expression = "java(channel.updatedAt().toEpochMilli())")
    ProviderChannelResponse toResponse(ProviderChannel channel);

    @Mapping(target = "id", expression = "java(modelSupport.id().value())")
    @Mapping(target = "requestedModel", expression = "java(modelSupport.requestedModel().value())")
    @Mapping(target = "upstreamModel", expression = "java(modelSupport.upstreamModel().value())")
    @Mapping(target = "upstreamProtocol", expression = "java(modelSupport.upstreamProtocol().name())")
    @Mapping(target = "priority", expression = "java(modelSupport.priority().value())")
    @Mapping(target = "status", expression = "java(modelSupport.status().name())")
    @Mapping(target = "source", expression = "java(modelSupport.source().name())")
    @Mapping(target = "createdAt", expression = "java(modelSupport.createdAt().toEpochMilli())")
    @Mapping(target = "updatedAt", expression = "java(modelSupport.updatedAt().toEpochMilli())")
    ChannelModelSupportResponse toModelSupportResponse(ChannelModelSupport modelSupport);

    default List<ChannelModelSupportResponse> toModelSupportResponses(List<ChannelModelSupport> modelSupports) {
        return modelSupports.stream().map(this::toModelSupportResponse).toList();
    }

    default Set<String> toProtocolNames(Set<ProtocolType> protocols) {
        return protocols.stream().map(ProtocolType::name).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default Set<ProtocolType> toProtocolTypes(Set<String> protocols) {
        return protocols.stream().map(this::toProtocolType).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default ProtocolType toProtocolType(String protocol) {
        return ProtocolType.valueOf(protocol.trim().toUpperCase().replace('-', '_'));
    }
}
