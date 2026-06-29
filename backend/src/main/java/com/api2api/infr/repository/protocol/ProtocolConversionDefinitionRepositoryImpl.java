package com.api2api.infr.repository.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.protocol.repository.ProtocolConversionDefinitionRepository;
import com.api2api.infr.repository.protocol.converter.ProtocolConversionDefinitionPersistenceConverter;
import com.api2api.infr.repository.protocol.mapper.ProtocolConversionDefinitionMapper;
import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProtocolConversionDefinitionRepositoryImpl implements ProtocolConversionDefinitionRepository {

    @NonNull
    private final ProtocolConversionDefinitionMapper mapper;

    @NonNull
    private final ProtocolConversionDefinitionPersistenceConverter converter;

    @Override
    public void save(ProtocolConversionDefinition definition) {
        Objects.requireNonNull(definition, "ProtocolConversionDefinition must not be null");
        ProtocolConversionDefinitionPO po = converter.toPO(definition);
        if (mapper.selectById(po.getId()) == null) {
            mapper.insert(po);
        } else {
            mapper.update(po);
        }
    }

    @Override
    public Optional<ProtocolConversionDefinition> findById(ProtocolConversionDefinitionId id) {
        Objects.requireNonNull(id, "ProtocolConversionDefinitionId must not be null");
        return Optional.ofNullable(mapper.selectById(id.value()))
                .map(converter::toDomain);
    }

    @Override
    public Optional<ProtocolConversionDefinition> findBySourceAndTarget(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        Objects.requireNonNull(sourceProtocol, "Source protocol must not be null");
        Objects.requireNonNull(targetProtocol, "Target protocol must not be null");
        return Optional.ofNullable(mapper.selectBySourceAndTarget(sourceProtocol.name(), targetProtocol.name()))
                .map(converter::toDomain);
    }

    @Override
    public List<ProtocolConversionDefinition> findAll() {
        return mapper.selectAll().stream()
                .map(converter::toDomain)
                .toList();
    }
}
