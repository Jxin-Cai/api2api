package com.api2api.domain.protocolmetadata.repository;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadataId;
import java.util.List;
import java.util.Optional;

public interface ProtocolMetadataRepository {

    List<ProtocolMetadata> findAll();

    Optional<ProtocolMetadata> findById(ProtocolMetadataId id);

    Optional<ProtocolMetadata> findByProtocolType(ProtocolType protocolType);
}
