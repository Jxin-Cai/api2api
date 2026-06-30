package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
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

    private final int routePriority;

    @NotEmpty
    private final Set<ProtocolType> supportedProtocols;

    @Builder
    private CreateProviderChannelCommand(
            UserAccountId operatorUserId,
            ProviderChannelId providerChannelId,
            ProviderChannelName name,
            ProviderHost host,
            ProviderKeyRef keyRef,
            int routePriority,
            Set<ProtocolType> supportedProtocols
    ) {
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.providerChannelId = Objects.requireNonNull(providerChannelId, "Provider channel id must not be null");
        this.name = Objects.requireNonNull(name, "Provider channel name must not be null");
        this.host = Objects.requireNonNull(host, "Provider host must not be null");
        this.keyRef = Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        this.routePriority = routePriority;
        this.supportedProtocols = copyNotEmpty(supportedProtocols);
    }

    private static Set<ProtocolType> copyNotEmpty(Set<ProtocolType> source) {
        Objects.requireNonNull(source, "Supported protocols must not be null");
        if (source.isEmpty()) {
            throw new IllegalArgumentException("Supported protocols must not be empty");
        }
        Set<ProtocolType> copied = new LinkedHashSet<>();
        for (ProtocolType protocol : source) {
            copied.add(Objects.requireNonNull(protocol, "Supported protocol must not be null"));
        }
        return Collections.unmodifiableSet(copied);
    }
}
