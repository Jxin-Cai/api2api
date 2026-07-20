package com.api2api.ohs.http.admin.converter;

import com.api2api.application.channel.command.BatchUpsertChannelModelsCommand;
import com.api2api.application.channel.command.ChangeChannelModelStatusCommand;
import com.api2api.application.channel.command.ChangeProviderChannelStatusCommand;
import com.api2api.application.channel.command.ChannelModelUpsertItemCommand;
import com.api2api.application.channel.command.CreateProviderChannelCommand;
import com.api2api.application.channel.command.FetchProviderChannelModelPreviewCommand;
import com.api2api.application.channel.command.FetchProviderModelPreviewCommand;
import com.api2api.application.channel.command.FetchProviderModelsCommand;
import com.api2api.application.channel.command.RemoveChannelModelCommand;
import com.api2api.application.channel.command.UpdateProviderChannelCommand;
import com.api2api.application.channel.command.UpsertChannelModelCommand;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ChannelProtocolMapping;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.admin.dto.AdminBatchUpsertChannelModelItemRequest;
import com.api2api.ohs.http.admin.dto.AdminBatchUpsertChannelModelsRequest;
import com.api2api.ohs.http.admin.dto.AdminCreateProviderChannelRequest;
import com.api2api.ohs.http.admin.dto.AdminFetchProviderChannelModelPreviewRequest;
import com.api2api.ohs.http.admin.dto.AdminFetchProviderModelPreviewRequest;
import com.api2api.ohs.http.admin.dto.AdminFetchProviderModelsRequest;
import com.api2api.ohs.http.admin.dto.AdminUpdateProviderChannelRequest;
import com.api2api.ohs.http.admin.dto.AdminUpsertChannelModelRequest;
import com.api2api.ohs.http.admin.dto.ChannelModelSupportResponse;
import com.api2api.ohs.http.admin.dto.ProtocolMappingRequest;
import com.api2api.ohs.http.admin.dto.ProtocolMappingResponse;
import com.api2api.ohs.http.admin.dto.ProviderChannelListResponse;
import com.api2api.ohs.http.admin.dto.ProviderChannelResponse;
import com.api2api.ohs.http.admin.dto.ProviderModelPreviewResponse;
import java.util.Arrays;
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
                .modelsPath(ProviderModelsPath.of(request.getModelsPath()))
                .routePriority(defaultRoutePriority(request.getRoutePriority()))
                .protocolMappings(toProtocolMappings(request.getProtocolMappings(), request.getSupportedProtocols()))
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
                .keyRef(hasText(request.getKeyRef()) ? ProviderKeyRef.of(request.getKeyRef()) : null)
                .modelsPath(ProviderModelsPath.of(request.getModelsPath()))
                .routePriority(defaultRoutePriority(request.getRoutePriority()))
                .protocolMappings(toProtocolMappings(request.getProtocolMappings(), request.getSupportedProtocols()))
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

    default ChangeChannelModelStatusCommand toChangeModelStatusCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId,
            ChannelModelSupportId channelModelSupportId
    ) {
        return ChangeChannelModelStatusCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .channelModelSupportId(channelModelSupportId)
                .build();
    }

    default FetchProviderModelPreviewCommand toFetchModelPreviewCommand(
            AdminFetchProviderModelPreviewRequest request,
            UserAccountId operatorUserId
    ) {
        return FetchProviderModelPreviewCommand.builder()
                .operatorUserId(operatorUserId)
                .host(ProviderHost.of(request.getHost()))
                .keyRef(ProviderKeyRef.of(request.getKeyRef()))
                .modelsPath(ProviderModelsPath.of(request.getModelsPath()))
                .upstreamProtocols(toPreviewUpstreamProtocols(request.getUpstreamProtocols(), request.getProtocolMappings(), request.getSupportedProtocols()))
                .defaultPriority(RoutePriority.of(request.getDefaultPriority()))
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

    default FetchProviderChannelModelPreviewCommand toFetchChannelModelPreviewCommand(
            AdminFetchProviderChannelModelPreviewRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        return FetchProviderChannelModelPreviewCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .host(hasText(request.getHost()) ? ProviderHost.of(request.getHost()) : null)
                .keyRef(hasText(request.getKeyRef()) ? ProviderKeyRef.of(request.getKeyRef()) : null)
                .modelsPath(hasText(request.getModelsPath()) ? ProviderModelsPath.of(request.getModelsPath()) : null)
                .upstreamProtocols(toOptionalPreviewUpstreamProtocols(request.getUpstreamProtocols(), request.getProtocolMappings(), request.getSupportedProtocols()))
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
                .preferred(Boolean.TRUE.equals(request.getPreferred()))
                .source(ModelSupportSource.valueOf(request.getSource()))
                .build();
    }

    default BatchUpsertChannelModelsCommand toBatchUpsertModelsCommand(
            AdminBatchUpsertChannelModelsRequest request,
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId
    ) {
        List<AdminBatchUpsertChannelModelItemRequest> models = request.getModels() == null
                ? List.of()
                : request.getModels();
        return BatchUpsertChannelModelsCommand.builder()
                .operatorUserId(operatorUserId)
                .providerChannelId(providerChannelId)
                .replaceExisting(Boolean.TRUE.equals(request.getReplaceExisting()))
                .models(models.stream().map(this::toBatchUpsertItemCommand).toList())
                .build();
    }

    default ChannelModelUpsertItemCommand toBatchUpsertItemCommand(AdminBatchUpsertChannelModelItemRequest request) {
        return ChannelModelUpsertItemCommand.builder()
                .channelModelSupportId(request.getId() == null || request.getId() <= 0 ? null : ChannelModelSupportId.of(request.getId()))
                .requestedModel(ModelName.of(request.getRequestedModel()))
                .upstreamModel(ModelName.of(request.getUpstreamModel()))
                .upstreamProtocol(toProtocolType(request.getUpstreamProtocol()))
                .priority(RoutePriority.of(request.getPriority()))
                .preferred(Boolean.TRUE.equals(request.getPreferred()))
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

    default ProviderModelPreviewResponse toPreviewResponse(List<ChannelModelSupport> models) {
        return ProviderModelPreviewResponse.builder()
                .models(toModelSupportResponses(models))
                .build();
    }

    @Mapping(target = "id", expression = "java(channel.id().value())")
    @Mapping(target = "name", expression = "java(channel.name().value())")
    @Mapping(target = "host", expression = "java(channel.host().value())")
    @Mapping(target = "keyRef", expression = "java(maskKey(channel.keyRef().value()))")
    @Mapping(target = "keyMasked", expression = "java(maskKey(channel.keyRef().value()))")
    @Mapping(target = "hasKey", expression = "java(hasText(channel.keyRef().value()))")
    @Mapping(target = "modelsPath", expression = "java(channel.modelsPath().value())")
    @Mapping(target = "routePriority", expression = "java(channel.routePriority())")
    @Mapping(target = "supportedProtocols", expression = "java(toProtocolNames(channel.supportedProtocols()))")
    @Mapping(target = "protocolMappings", expression = "java(toProtocolMappingResponses(channel.protocolMappings()))")
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
    @Mapping(target = "preferred", expression = "java(modelSupport.preferred())")
    @Mapping(target = "status", expression = "java(modelSupport.status().name())")
    @Mapping(target = "rateLimitedAt", expression = "java(toEpochMilli(modelSupport.rateLimitedAt()))")
    @Mapping(target = "rateLimitResetAt", expression = "java(toEpochMilli(modelSupport.rateLimitResetAt()))")
    @Mapping(target = "source", expression = "java(modelSupport.source().name())")
    @Mapping(target = "createdAt", expression = "java(modelSupport.createdAt().toEpochMilli())")
    @Mapping(target = "updatedAt", expression = "java(modelSupport.updatedAt().toEpochMilli())")
    ChannelModelSupportResponse toModelSupportResponse(ChannelModelSupport modelSupport);

    default Long toEpochMilli(java.time.Instant value) {
        return value == null ? null : value.toEpochMilli();
    }

    default List<ChannelModelSupportResponse> toModelSupportResponses(List<ChannelModelSupport> modelSupports) {
        return modelSupports.stream().map(this::toModelSupportResponse).toList();
    }

    default List<ProtocolMappingResponse> toProtocolMappingResponses(Set<ChannelProtocolMapping> mappings) {
        return mappings.stream()
                .map(mapping -> ProtocolMappingResponse.builder()
                        .requestProtocol(mapping.requestProtocol().name())
                        .upstreamProtocol(mapping.upstreamProtocol().name())
                        .build())
                .toList();
    }

    default Set<String> toProtocolNames(Set<ProtocolType> protocols) {
        return protocols.stream().map(ProtocolType::name).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default Set<ProtocolType> toProtocolTypes(Set<String> protocols) {
        if (protocols == null || protocols.isEmpty()) {
            throw new IllegalArgumentException("supportedProtocols must not be empty");
        }
        return protocols.stream().map(this::toProtocolType).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default Set<ProtocolType> toPreviewUpstreamProtocols(
            Set<String> upstreamProtocols,
            List<ProtocolMappingRequest> mappings,
            Set<String> fallbackProtocols
    ) {
        Set<ProtocolType> protocols = toOptionalPreviewUpstreamProtocols(upstreamProtocols, mappings, fallbackProtocols);
        if (protocols == null || protocols.isEmpty()) {
            throw new IllegalArgumentException("upstreamProtocols must not be empty");
        }
        return protocols;
    }

    default Set<ProtocolType> toOptionalPreviewUpstreamProtocols(
            Set<String> upstreamProtocols,
            List<ProtocolMappingRequest> mappings,
            Set<String> fallbackProtocols
    ) {
        if (upstreamProtocols != null && !upstreamProtocols.isEmpty()) {
            return upstreamProtocols.stream()
                    .map(this::toProtocolType)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (mappings != null && !mappings.isEmpty()) {
            return mappings.stream()
                    .map(ProtocolMappingRequest::getUpstreamProtocol)
                    .map(this::toProtocolType)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (fallbackProtocols != null && !fallbackProtocols.isEmpty()) {
            return fallbackProtocols.stream()
                    .map(this::toProtocolType)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return null;
    }

    default Set<ChannelProtocolMapping> toProtocolMappings(List<ProtocolMappingRequest> mappings, Set<String> fallbackProtocols) {
        Set<ProtocolType> supportedProtocols = toProtocolTypes(fallbackProtocols);
        if (mappings == null || mappings.isEmpty()) {
            return supportedProtocols.stream()
                    .map(protocol -> ChannelProtocolMapping.of(protocol, protocol))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        Set<ChannelProtocolMapping> protocolMappings = mappings.stream()
                .map(mapping -> ChannelProtocolMapping.of(
                        toProtocolType(mapping.getRequestProtocol()),
                        toProtocolType(mapping.getUpstreamProtocol())
                ))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertRequestProtocolsSupported(protocolMappings, supportedProtocols);
        return protocolMappings;
    }

    default Set<ChannelProtocolMapping> toOptionalProtocolMappings(List<ProtocolMappingRequest> mappings, Set<String> fallbackProtocols) {
        if ((mappings == null || mappings.isEmpty()) && (fallbackProtocols == null || fallbackProtocols.isEmpty())) {
            return null;
        }
        return toProtocolMappings(mappings, fallbackProtocols);
    }

    default void assertRequestProtocolsSupported(Set<ChannelProtocolMapping> mappings, Set<ProtocolType> supportedProtocols) {
        Set<ProtocolType> requestProtocols = mappings.stream()
                .map(ChannelProtocolMapping::requestProtocol)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!supportedProtocols.containsAll(requestProtocols)) {
            throw new IllegalArgumentException("Protocol mappings request protocols must be contained in supportedProtocols");
        }
    }

    default int defaultRoutePriority(Integer routePriority) {
        return routePriority == null ? 0 : routePriority;
    }

    default boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    default String maskKey(String value) {
        if (!hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    default ProtocolType toProtocolType(String protocol) {
        if (!hasText(protocol)) {
            throw new IllegalArgumentException("Protocol must not be blank. Supported protocols: " + supportedProtocolNames());
        }
        String normalized = protocol.trim().toUpperCase().replace('-', '_');
        try {
            return ProtocolType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported protocol '" + protocol + "'. Supported protocols: " + supportedProtocolNames(), exception);
        }
    }

    default String supportedProtocolNames() {
        return Arrays.stream(ProtocolType.values()).map(ProtocolType::name).collect(Collectors.joining(", "));
    }
}
