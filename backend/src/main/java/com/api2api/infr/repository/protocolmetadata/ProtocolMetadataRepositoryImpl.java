package com.api2api.infr.repository.protocolmetadata;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinition;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinitionId;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadataId;
import com.api2api.domain.protocolmetadata.repository.ProtocolMetadataRepository;
import com.api2api.infr.protocol.contract.ProtocolContract;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.api2api.infr.protocol.contract.ProtocolFieldRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ProtocolMetadataRepositoryImpl implements ProtocolMetadataRepository {

    private static final Instant CONTRACT_EPOCH = Instant.EPOCH;

    private final Map<ProtocolType, ProtocolMetadata> metadataByType = new EnumMap<>(ProtocolType.class);
    private final Map<ProtocolMetadataId, ProtocolMetadata> metadataById = new LinkedHashMap<>();

    public ProtocolMetadataRepositoryImpl(@NonNull ProtocolContractRegistry registry) {
        long metadataId = 1;
        long fieldId = 1;
        for (ProtocolContract contract : registry.contracts()) {
            List<ProtocolFieldDefinition> definitions = new ArrayList<>();
            int sortOrder = 1;
            for (ProtocolFieldRef field : contract.fields()) {
                definitions.add(ProtocolFieldDefinition.of(
                        ProtocolFieldDefinitionId.of(fieldId++),
                        field.fieldName(),
                        field.path(),
                        field.type(),
                        field.required(),
                        field.section(),
                        field.direction(),
                        field.description(),
                        field.purpose(),
                        field.usageContext(),
                        sortOrder++
                ));
            }
            ProtocolMetadata metadata = ProtocolMetadata.rehydrate(
                    ProtocolMetadataId.of(metadataId++),
                    contract.protocolType(),
                    contract.displayName(),
                    contract.apiSpecVersion(),
                    contract.description(),
                    contract.defaultEndpointPath(),
                    definitions,
                    CONTRACT_EPOCH,
                    CONTRACT_EPOCH
            );
            metadataByType.put(contract.protocolType(), metadata);
            metadataById.put(metadata.id(), metadata);
        }
    }

    @Override
    public List<ProtocolMetadata> findAll() {
        return List.copyOf(metadataByType.values());
    }

    @Override
    public Optional<ProtocolMetadata> findById(ProtocolMetadataId id) {
        return Optional.ofNullable(metadataById.get(id));
    }

    @Override
    public Optional<ProtocolMetadata> findByProtocolType(ProtocolType protocolType) {
        return Optional.ofNullable(metadataByType.get(protocolType));
    }
}
