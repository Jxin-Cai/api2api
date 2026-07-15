package com.api2api.infr.protocol.contract;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolcontract.acl.ExecutableProtocolContract;
import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public final class ProtocolContractRegistry implements ExecutableProtocolContract {

    private final Map<ProtocolType, ProtocolContract> contracts;

    public ProtocolContractRegistry(ObjectMapper objectMapper) {
        EnumMap<ProtocolType, ProtocolContract> indexed = new EnumMap<>(ProtocolType.class);
        for (ProtocolContract contract : ProtocolContractDefinitions.create(objectMapper)) {
            ProtocolContract previous = indexed.put(contract.protocolType(), contract);
            if (previous != null) {
                throw new IllegalStateException("Duplicate protocol contract: " + contract.protocolType());
            }
        }
        this.contracts = Map.copyOf(indexed);
    }

    @Override
    public ParsedGatewayRequest parseRequest(ProtocolType protocolType, String rawBody) {
        return require(protocolType).parseRequest(rawBody);
    }

    @Override
    public String buildResponse(ProtocolType protocolType, String rawBody) {
        return require(protocolType).buildResponse(rawBody);
    }

    public ProtocolContract require(ProtocolType protocolType) {
        ProtocolContract contract = contracts.get(protocolType);
        if (contract == null) {
            throw new IllegalArgumentException("Unsupported protocol contract: " + protocolType);
        }
        return contract;
    }

    public List<ProtocolContract> contracts() {
        return contracts.values().stream()
                .sorted((left, right) -> left.protocolType().compareTo(right.protocolType()))
                .toList();
    }
}
