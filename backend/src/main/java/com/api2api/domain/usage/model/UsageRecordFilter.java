package com.api2api.domain.usage.model;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserRole;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable filter used to query usage records with role-based visibility constraints.
 */
public final class UsageRecordFilter {

    private final UserAccountId userAccountId;
    private final ApiCredentialId apiCredentialId;
    private final ModelName requestedModel;
    private final ProviderChannelId providerChannelId;
    private final ProtocolType requestProtocol;
    private final UsageTimeRange timeRange;
    private final UserRole viewerRole;

    private UsageRecordFilter(
            UserAccountId userAccountId,
            ApiCredentialId apiCredentialId,
            ModelName requestedModel,
            ProviderChannelId providerChannelId,
            ProtocolType requestProtocol,
            UsageTimeRange timeRange,
            UserRole viewerRole
    ) {
        this.timeRange = Objects.requireNonNull(timeRange, "Usage time range must not be null");
        this.viewerRole = Objects.requireNonNull(viewerRole, "Viewer role must not be null");
        if (this.viewerRole == UserRole.USER && userAccountId == null) {
            throw new IllegalArgumentException("User portal usage filter must be bound to the current user");
        }
        if (this.viewerRole == UserRole.USER && providerChannelId != null) {
            throw new IllegalArgumentException("User portal usage filter must not include provider channel");
        }
        this.userAccountId = userAccountId;
        this.apiCredentialId = apiCredentialId;
        this.requestedModel = requestedModel;
        this.providerChannelId = providerChannelId;
        this.requestProtocol = requestProtocol;
    }

    public static UsageRecordFilter of(
            UserAccountId userAccountId,
            ApiCredentialId apiCredentialId,
            ModelName requestedModel,
            ProviderChannelId providerChannelId,
            ProtocolType requestProtocol,
            UsageTimeRange timeRange,
            UserRole viewerRole
    ) {
        return new UsageRecordFilter(
                userAccountId,
                apiCredentialId,
                requestedModel,
                providerChannelId,
                requestProtocol,
                timeRange,
                viewerRole
        );
    }

    public static UsageRecordFilter forUserPortal(
            UserAccountId currentUserId,
            ApiCredentialId apiCredentialId,
            ModelName requestedModel,
            ProtocolType requestProtocol,
            UsageTimeRange timeRange
    ) {
        return new UsageRecordFilter(
                Objects.requireNonNull(currentUserId, "Current user id must not be null"),
                apiCredentialId,
                requestedModel,
                null,
                requestProtocol,
                timeRange,
                UserRole.USER
        );
    }

    public boolean isUserPortalView() {
        return viewerRole == UserRole.USER;
    }

    public boolean isAdminView() {
        return viewerRole == UserRole.ADMIN;
    }

    public boolean canFilterProviderChannel() {
        return viewerRole == UserRole.ADMIN;
    }

    public Optional<UserAccountId> userAccountIdOption() {
        return Optional.ofNullable(userAccountId);
    }

    public Optional<ApiCredentialId> apiCredentialIdOption() {
        return Optional.ofNullable(apiCredentialId);
    }

    public Optional<ModelName> requestedModelOption() {
        return Optional.ofNullable(requestedModel);
    }

    public Optional<ProviderChannelId> providerChannelIdOption() {
        return Optional.ofNullable(providerChannelId);
    }

    public Optional<ProtocolType> requestProtocolOption() {
        return Optional.ofNullable(requestProtocol);
    }

    public UserAccountId userAccountId() {
        return userAccountId;
    }

    public ApiCredentialId apiCredentialId() {
        return apiCredentialId;
    }

    public ModelName requestedModel() {
        return requestedModel;
    }

    public ProviderChannelId providerChannelId() {
        return providerChannelId;
    }

    public ProtocolType requestProtocol() {
        return requestProtocol;
    }

    public UsageTimeRange timeRange() {
        return timeRange;
    }

    public UserRole viewerRole() {
        return viewerRole;
    }

    public UserAccountId getUserAccountId() {
        return userAccountId;
    }

    public ApiCredentialId getApiCredentialId() {
        return apiCredentialId;
    }

    public ModelName getRequestedModel() {
        return requestedModel;
    }

    public ProviderChannelId getProviderChannelId() {
        return providerChannelId;
    }

    public ProtocolType getRequestProtocol() {
        return requestProtocol;
    }

    public UsageTimeRange getTimeRange() {
        return timeRange;
    }

    public UserRole getViewerRole() {
        return viewerRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageRecordFilter that)) {
            return false;
        }
        return Objects.equals(userAccountId, that.userAccountId)
                && Objects.equals(apiCredentialId, that.apiCredentialId)
                && Objects.equals(requestedModel, that.requestedModel)
                && Objects.equals(providerChannelId, that.providerChannelId)
                && requestProtocol == that.requestProtocol
                && Objects.equals(timeRange, that.timeRange)
                && viewerRole == that.viewerRole;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                userAccountId,
                apiCredentialId,
                requestedModel,
                providerChannelId,
                requestProtocol,
                timeRange,
                viewerRole
        );
    }
}
