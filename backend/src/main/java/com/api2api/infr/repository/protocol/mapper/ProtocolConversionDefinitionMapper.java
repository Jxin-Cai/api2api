package com.api2api.infr.repository.protocol.mapper;

import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
import java.util.List;

public interface ProtocolConversionDefinitionMapper {
    int insert(ProtocolConversionDefinitionPO definition);
    int update(ProtocolConversionDefinitionPO definition);
    ProtocolConversionDefinitionPO selectById(Long id);
    ProtocolConversionDefinitionPO selectBySourceAndTarget(String sourceProtocol, String targetProtocol);
    List<ProtocolConversionDefinitionPO> selectAll();
}
