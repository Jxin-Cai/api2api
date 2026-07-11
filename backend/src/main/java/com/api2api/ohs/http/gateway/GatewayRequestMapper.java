package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.gateway.model.GatewayInvocationId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.UsageRecordId;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Maps protocol gateway request to InvokeGatewayCommand for application layer.
 */
@Component
@RequiredArgsConstructor
public class GatewayRequestMapper {

    @NonNull
    private final GatewayApiKeyHashHelper apiKeyHashHelper;

    @NonNull
    private final GatewayIdentifierHelper identifierHelper;

    public InvokeGatewayCommand toCommand(
            GatewayProtocolRequest protocolRequest,
            String authorizationHeader,
            String apiKeyHeader,
            String xRequestId,
            ProtocolType requestProtocol,
            HttpHeaders incomingHeaders
    ) {
        Objects.requireNonNull(protocolRequest, "Protocol request must not be null");
        Objects.requireNonNull(requestProtocol, "Request protocol must not be null");

        ApiKeyHash keyHash = apiKeyHashHelper.hashGatewayApiKey(authorizationHeader, apiKeyHeader);
        GatewayInvocationId gatewayInvocationId = identifierHelper.nextInvocationId();
        GatewayRequestId gatewayRequestId = identifierHelper.requestId(xRequestId);
        UsageRecordId usageRecordId = identifierHelper.nextUsageRecordId();

        String modelValue = protocolRequest.model();
        if (modelValue == null || modelValue.isBlank()) {
            throw new IllegalArgumentException("Model is required in protocol request");
        }

        com.api2api.domain.credential.model.ModelName requestedCredentialModel =
                com.api2api.domain.credential.model.ModelName.of(modelValue);
        com.api2api.domain.channel.model.ModelName requestedModel =
                com.api2api.domain.channel.model.ModelName.of(modelValue);

        return InvokeGatewayCommand.builder()
                .gatewayInvocationId(gatewayInvocationId)
                .gatewayRequestId(gatewayRequestId)
                .usageRecordId(usageRecordId)
                .keyHash(keyHash)
                .requestedCredentialModel(requestedCredentialModel)
                .requestedModel(requestedModel)
                .requestProtocol(requestProtocol)
                .requestBody(protocolRequest.rawBody())
                .incomingHeaders(incomingHeaders)
                .streaming(protocolRequest.streaming())
                .toolCallingRequired(protocolRequest.toolCallingRequired())
                .reasoningRequired(protocolRequest.reasoningRequired())
                .build();
    }
}
