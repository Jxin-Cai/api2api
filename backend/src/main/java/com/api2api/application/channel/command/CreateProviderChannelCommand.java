package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ChannelProtocolMapping;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
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
 * Command for creating a provider channel.
 */
@Getter
public final class CreateProviderChannelCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProviderChannelId providerChannelId;

    @NotNull
    private final ProviderChannelName name;

    @NotNull
    private final ProviderHost host;

    @NotNull
    private final ProviderKeyRef keyRef;

    @NotNull
    private final ProviderModelsPath modelsPath;

    private final int routePriority;

    @NotEmpty
    private final Set<ChannelProtocolMapping> protocolMappings;

    @Builder
    private CreateProviderChannelCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            ProviderModelsPath modelsPath,
            int routePriority,
            Set<ChannelProtocolMapping> protocolMappings
    ) {
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.name = Objects.requireNonNull(name, "Provider channel name must not be null");
        this.host = Objects.requireNonNull(host, "Provider host must not be null");
        this.keyRef = Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        this.modelsPath = Objects.requireNonNullElse(modelsPath, ProviderModelsPath.DEFAULT);
        this.routePriority = routePriority;
        this.protocolMappings = copyNotEmpty(protocolMappings);
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
