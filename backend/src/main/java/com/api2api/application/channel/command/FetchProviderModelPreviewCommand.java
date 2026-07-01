package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ChannelProtocolMapping;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for previewing upstream provider models without persisting them.
 */
@Getter
public final class FetchProviderModelPreviewCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderHost host;

    @NotNull
    private final ProviderKeyRef keyRef;

    @NotNull
    private final ProviderModelsPath modelsPath;

    @NotEmpty
    private final Set<ChannelProtocolMapping> protocolMappings;

    @NotNull
    private final RoutePriority defaultPriority;

    @Builder
    private FetchProviderModelPreviewCommand(
            UserAccountId operatorUserId,
            ProviderHost host,
            ProviderKeyRef keyRef,
            ProviderModelsPath modelsPath,
            Set<ChannelProtocolMapping> protocolMappings,
            RoutePriority defaultPriority
    ) {
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.host = Objects.requireNonNull(host, "Provider host must not be null");
        this.keyRef = Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        this.modelsPath = Objects.requireNonNullElse(modelsPath, ProviderModelsPath.DEFAULT);
        this.protocolMappings = copyNotEmpty(protocolMappings);
        this.defaultPriority = Objects.requireNonNull(defaultPriority, "Default priority must not be null");
    }

    private static Set<ChannelProtocolMapping> copyNotEmpty(Set<ChannelProtocolMapping> source) {
        Objects.requireNonNull(source, "Protocol mappings must not be null");
        if (source.isEmpty()) {
            throw new IllegalArgumentException("Protocol mappings must not be empty");
        }
        Set<ChannelProtocolMapping> copied = new LinkedHashSet<>();
        for (ChannelProtocolMapping mapping : source) {
            copied.add(Objects.requireNonNull(mapping, "Protocol mapping must not be null"));
        }
        return Collections.unmodifiableSet(copied);
    }
}
