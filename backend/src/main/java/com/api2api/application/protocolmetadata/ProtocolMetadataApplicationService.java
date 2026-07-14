package com.api2api.application.protocolmetadata;

import com.api2api.application.BusinessException;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadataId;
import com.api2api.domain.protocolmetadata.repository.ProtocolMetadataRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProtocolMetadataApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final ProtocolMetadataRepository protocolMetadataRepository;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ProtocolMetadata> listAll(UserAccountId operatorUserId) {
        assertAdminBackofficeAccess(operatorUserId);
        return protocolMetadataRepository.findAll();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProtocolMetadata getByProtocolType(UserAccountId operatorUserId, ProtocolType protocolType) {
        assertAdminBackofficeAccess(operatorUserId);
        return protocolMetadataRepository.findByProtocolType(protocolType)
                .orElseThrow(() -> new BusinessException("PROTOCOL_METADATA_NOT_FOUND"));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ProtocolMetadata getById(UserAccountId operatorUserId, ProtocolMetadataId id) {
        assertAdminBackofficeAccess(operatorUserId);
        return protocolMetadataRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PROTOCOL_METADATA_NOT_FOUND"));
    }

    private void assertAdminBackofficeAccess(UserAccountId operatorUserId) {
        UserAccount operator = userAccountRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }
}
