package com.api2api.application.gateway.command;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.gateway.model.GatewayInvocationId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.UsageRecordId;
import java.util.Objects;

/**
 * Immutable command carrying one gateway invocation request.
 */
public final class InvokeGatewayCommand {

    private final GatewayInvocationId gatewayInvocationId;
    private final GatewayRequestId gatewayRequestId;
    private final UsageRecordId usageRecordId;
    private final ApiKeyHash keyHash;
    private final com.api2api.domain.credential.model.ModelName requestedCredentialModel;
    private final com.api2api.domain.channel.model.ModelName requestedModel;
    private final ProtocolType requestProtocol;
    private final String requestBody;
    private final boolean streaming;
    private final boolean toolCallingRequired;
    private final boolean reasoningRequired;

    private InvokeGatewayCommand(Builder builder) {
        this.gatewayInvocationId = Objects.requireNonNull(builder.gatewayInvocationId, "Gateway invocation id must not be null");
        this.gatewayRequestId = Objects.requireNonNull(builder.gatewayRequestId, "Gateway request id must not be null");
        this.usageRecordId = Objects.requireNonNull(builder.usageRecordId, "Usage record id must not be null");
        this.keyHash = Objects.requireNonNull(builder.keyHash, "API key hash must not be null");
        this.requestedCredentialModel = Objects.requireNonNull(builder.requestedCredentialModel, "Requested credential model must not be null");
        this.requestedModel = Objects.requireNonNull(builder.requestedModel, "Requested model must not be null");
        this.requestProtocol = Objects.requireNonNull(builder.requestProtocol, "Request protocol must not be null");
        this.requestBody = requireNotBlank(builder.requestBody, "Request body must not be blank");
        this.streaming = builder.streaming;
        this.toolCallingRequired = builder.toolCallingRequired;
        this.reasoningRequired = builder.reasoningRequired;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireNotBlank(String value, String message) {
        String required = Objects.requireNonNull(value, message).trim();
        if (required.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public GatewayInvocationId getGatewayInvocationId() {
        return gatewayInvocationId;
    }

    public GatewayRequestId getGatewayRequestId() {
        return gatewayRequestId;
    }

    public UsageRecordId getUsageRecordId() {
        return usageRecordId;
    }

    public ApiKeyHash getKeyHash() {
        return keyHash;
    }

    public com.api2api.domain.credential.model.ModelName getRequestedCredentialModel() {
        return requestedCredentialModel;
    }

    public com.api2api.domain.channel.model.ModelName getRequestedModel() {
        return requestedModel;
    }

    public ProtocolType getRequestProtocol() {
        return requestProtocol;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public boolean isToolCallingRequired() {
        return toolCallingRequired;
    }

    public boolean isReasoningRequired() {
        return reasoningRequired;
    }

    public static final class Builder {
        private GatewayInvocationId gatewayInvocationId;
        private GatewayRequestId gatewayRequestId;
        private UsageRecordId usageRecordId;
        private ApiKeyHash keyHash;
        private com.api2api.domain.credential.model.ModelName requestedCredentialModel;
        private com.api2api.domain.channel.model.ModelName requestedModel;
        private ProtocolType requestProtocol;
        private String requestBody;
        private boolean streaming;
        private boolean toolCallingRequired;
        private boolean reasoningRequired;

        private Builder() {
        }

        public Builder gatewayInvocationId(GatewayInvocationId gatewayInvocationId) {
            this.gatewayInvocationId = gatewayInvocationId;
            return this;
        }

        public Builder gatewayRequestId(GatewayRequestId gatewayRequestId) {
            this.gatewayRequestId = gatewayRequestId;
            return this;
        }

        public Builder usageRecordId(UsageRecordId usageRecordId) {
            this.usageRecordId = usageRecordId;
            return this;
        }

        public Builder keyHash(ApiKeyHash keyHash) {
            this.keyHash = keyHash;
            return this;
        }

        public Builder requestedCredentialModel(com.api2api.domain.credential.model.ModelName requestedCredentialModel) {
            this.requestedCredentialModel = requestedCredentialModel;
            return this;
        }

        public Builder requestedModel(com.api2api.domain.channel.model.ModelName requestedModel) {
            this.requestedModel = requestedModel;
            return this;
        }

        public Builder requestProtocol(ProtocolType requestProtocol) {
            this.requestProtocol = requestProtocol;
            return this;
        }

        public Builder requestBody(String requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder toolCallingRequired(boolean toolCallingRequired) {
            this.toolCallingRequired = toolCallingRequired;
            return this;
        }

        public Builder reasoningRequired(boolean reasoningRequired) {
            this.reasoningRequired = reasoningRequired;
            return this;
        }

        public InvokeGatewayCommand build() {
            return new InvokeGatewayCommand(this);
        }
    }
}
