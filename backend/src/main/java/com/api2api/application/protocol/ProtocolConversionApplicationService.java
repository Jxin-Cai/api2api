package com.api2api.application.protocol;

import com.api2api.application.BusinessException;
import com.api2api.application.protocol.command.ChangeProtocolConversionStatusCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.protocol.repository.ProtocolConversionDefinitionRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProtocolConversionApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final ProtocolConversionDefinitionRepository conversionDefinitionRepository;

    @NonNull
    private final Clock clock;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ProtocolConversionDefinition> listDefinitions(UserAccountId operatorUserId) {
        assertAdminBackofficeAccess(operatorUserId);
        return conversionDefinitionRepository.findAll();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProtocolConversionDefinition getDefinition(
            UserAccountId operatorUserId,
            ProtocolConversionDefinitionId definitionId
    ) {
        assertAdminBackofficeAccess(operatorUserId);
        return loadDefinition(definitionId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProtocolConversionDefinition getDefinitionByDirection(
            UserAccountId operatorUserId,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol
    ) {
        assertAdminBackofficeAccess(operatorUserId);
        return conversionDefinitionRepository.findBySourceAndTarget(sourceProtocol, targetProtocol)
                .orElseThrow(() -> new BusinessException("PROTOCOL_CONVERSION_NOT_FOUND"));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProtocolConversionDefinition enableConversion(ChangeProtocolConversionStatusCommand command) {
        assertAdminBackofficeAccess(command.getOperatorUserId());
        ProtocolConversionDefinition definition = loadDefinition(command.getDefinitionId());
        definition.enable(Instant.now(clock));
        conversionDefinitionRepository.save(definition);
        return definition;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProtocolConversionDefinition disableConversion(ChangeProtocolConversionStatusCommand command) {
        assertAdminBackofficeAccess(command.getOperatorUserId());
        ProtocolConversionDefinition definition = loadDefinition(command.getDefinitionId());
        definition.disable(Instant.now(clock));
        conversionDefinitionRepository.save(definition);
        return definition;
    }

    private void assertAdminBackofficeAccess(UserAccountId operatorUserId) {
        UserAccount operator = userAccountRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }

    private ProtocolConversionDefinition loadDefinition(ProtocolConversionDefinitionId definitionId) {
        return conversionDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new BusinessException("PROTOCOL_CONVERSION_NOT_FOUND"));
    }
}
