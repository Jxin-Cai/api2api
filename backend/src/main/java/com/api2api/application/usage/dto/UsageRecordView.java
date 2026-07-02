package com.api2api.application.usage.dto;

import com.api2api.domain.usage.model.UsageRecord;
import java.util.Objects;

public final class UsageRecordView {

    private final UsageRecord record;
    private final String username;
    private final String apiCredentialName;
    private final String providerChannelName;

    private UsageRecordView(UsageRecord record, String username, String apiCredentialName, String providerChannelName) {
        this.record = Objects.requireNonNull(record, "Usage record must not be null");
        this.username = username;
        this.apiCredentialName = apiCredentialName;
        this.providerChannelName = providerChannelName;
    }

    public static UsageRecordView of(UsageRecord record, String username, String apiCredentialName, String providerChannelName) {
        return new UsageRecordView(record, username, apiCredentialName, providerChannelName);
    }

    public UsageRecord record() {
        return record;
    }

    public String username() {
        return username;
    }

    public String apiCredentialName() {
        return apiCredentialName;
    }

    public String providerChannelName() {
        return providerChannelName;
    }

    public UsageRecord getRecord() {
        return record;
    }

    public String getUsername() {
        return username;
    }

    public String getApiCredentialName() {
        return apiCredentialName;
    }

    public String getProviderChannelName() {
        return providerChannelName;
    }
}
