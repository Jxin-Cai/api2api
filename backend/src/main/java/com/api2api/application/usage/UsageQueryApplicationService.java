package com.api2api.application.usage;

import com.api2api.application.BusinessException;
import com.api2api.application.usage.command.QueryAdminUsageRecordsCommand;
import com.api2api.application.usage.command.QueryMyUsageRecordsCommand;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.usage.model.PageRequestSpec;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.usage.model.UsageTimeRange;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserRole;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageQueryApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final ApiCredentialRepository apiCredentialRepository;

    @NonNull
    private final UsageRecordRepository usageRecordRepository;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PagedUsageRecords queryMyUsageRecords(QueryMyUsageRecordsCommand command) {
        UserAccount currentUser = userAccountRepository.findById(command.getCurrentUserId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        currentUser.assertCanAccess(AccessScope.USER_PORTAL);

        if (command.getApiCredentialId() != null) {
            ApiCredential apiCredential = apiCredentialRepository.findById(command.getApiCredentialId())
                    .orElseThrow(() -> new BusinessException("API_CREDENTIAL_NOT_FOUND"));
            apiCredential.assertOwnedBy(command.getCurrentUserId());
        }

        UsageTimeRange timeRange = UsageTimeRange.of(
                command.getStartInclusive(),
                command.getEndExclusive()
        );
        PageRequestSpec pageRequest = PageRequestSpec.of(command.getPage(), command.getSize());
        UsageRecordFilter filter = UsageRecordFilter.forUserPortal(
                command.getCurrentUserId(),
                command.getApiCredentialId(),
                command.getRequestedModel(),
                command.getRequestProtocol(),
                timeRange
        );
        PagedUsageRecords page = usageRecordRepository.query(filter, pageRequest);
        List<UsageRecord> redactedRecords = page.getRecords().stream()
                .map(UsageRecord::redactForUserPortal)
                .toList();

        return PagedUsageRecords.of(
                redactedRecords,
                page.getPage(),
                page.getSize(),
                page.getTotalElements(),
                page.getFilteredTokenTotal()
        );
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PagedUsageRecords queryAdminUsageRecords(QueryAdminUsageRecordsCommand command) {
        UserAccount operator = userAccountRepository.findById(command.getOperatorUserId())
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);

        UsageTimeRange timeRange = UsageTimeRange.of(
                command.getStartInclusive(),
                command.getEndExclusive()
        );
        PageRequestSpec pageRequest = PageRequestSpec.of(command.getPage(), command.getSize());
        UsageRecordFilter filter = UsageRecordFilter.of(
                command.getUserAccountId(),
                command.getApiCredentialId(),
                command.getRequestedModel(),
                command.getProviderChannelId(),
                command.getRequestProtocol(),
                timeRange,
                UserRole.ADMIN
        );

        return usageRecordRepository.query(filter, pageRequest);
    }
}
