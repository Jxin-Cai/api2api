package com.api2api.application.channel.command;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
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

    @NotEmpty
    private final Set<ProtocolType> supportedProtocols;

    @NotNull
    private final RoutePriority defaultPriority;

    @Builder
    private FetchProviderModelPreviewCommand(
            UserAccountId operatorUserId,
            ProviderHost host,
            ProviderKeyRef keyRef,
            Set<ProtocolType> supportedProtocols,
            RoutePriority defaultPriority
    ) {
        this.operatorUserId = Objects.requireNonNull(operatorUserId, "Operator user id must not be null");
        this.host = Objects.requireNonNull(host, "Provider host must not be null");
        this.keyRef = Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        this.supportedProtocols = copyNotEmpty(supportedProtocols);
        this.defaultPriority = Objects.requireNonNull(defaultPriority, "Default priority must not be null");
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
