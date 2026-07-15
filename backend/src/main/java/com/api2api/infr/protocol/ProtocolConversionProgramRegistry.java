package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.infr.protocol.conversion.ProtocolConversionProgram;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class ProtocolConversionProgramRegistry {

    private final Map<DirectionKey, ProtocolConversionProgram> programIndex;

    public ProtocolConversionProgramRegistry(List<ProtocolMessageConverter> converters) {
        Map<DirectionKey, ProtocolConversionProgram> index = new HashMap<>();
        for (ProtocolMessageConverter converter : converters) {
            ProtocolConversionProgram program = converter.conversionProgram();
            DirectionKey key = new DirectionKey(
                    program.sourceProtocol(),
                    program.targetProtocol(),
                    program.direction()
            );
            ProtocolConversionProgram previous = index.put(key, program);
            if (previous != null) {
                throw new IllegalStateException("Duplicate protocol conversion program: " + key);
            }
        }
        this.programIndex = Collections.unmodifiableMap(index);
    }

    public Optional<List<FieldMapping>> describeRequestMappings(ProtocolType source, ProtocolType target) {
        return findProgram(source, target, ProtocolConversionDirection.REQUEST)
                .map(ProtocolConversionProgram::fieldMappings);
    }

    public Optional<List<FieldMapping>> describeResponseMappings(ProtocolType source, ProtocolType target) {
        return findProgram(source, target, ProtocolConversionDirection.RESPONSE)
                .map(ProtocolConversionProgram::fieldMappings);
    }

    public boolean hasConverter(ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {
        return findProgram(source, target, direction).isPresent();
    }

    public Optional<ProtocolConversionProgram> findProgram(
            ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {
        return Optional.ofNullable(programIndex.get(new DirectionKey(source, target, direction)));
    }

    private record DirectionKey(ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {}
}
