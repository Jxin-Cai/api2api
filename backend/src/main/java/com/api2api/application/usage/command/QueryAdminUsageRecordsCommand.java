package com.api2api.application.usage.command;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for querying platform-wide usage records from the admin back-office.
 */
@Getter
@Builder
public final class QueryAdminUsageRecordsCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    private final UserAccountId userAccountId;

    private final ApiCredentialId apiCredentialId;

    private final ModelName requestedModel;

    private final ProviderChannelId providerChannelId;

    private final ProtocolType requestProtocol;

    @NotNull
    private final Instant startInclusive;

    @NotNull
    private final Instant endExclusive;

    @Min(1)
    private final int page;

    private final int size;
}
