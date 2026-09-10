package com.api2api.application.usage;

import com.api2api.application.BusinessException;
import com.api2api.application.usage.command.QueryAdminUsageRecordsCommand;
import com.api2api.application.usage.command.QueryMyUsageRecordsCommand;
import com.api2api.application.usage.dto.PagedUsageRecordViews;
import com.api2api.application.usage.dto.UsageRecordView;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
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
    private final ProviderChannelRepository providerChannelRepository;

    @NonNull
    private final UsageRecordRepository usageRecordRepository;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PagedUsageRecordViews queryMyUsageRecords(QueryMyUsageRecordsCommand command) {
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
        return PagedUsageRecordViews.of(toViews(page.getRecords(), false), page);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PagedUsageRecordViews queryAdminUsageRecords(QueryAdminUsageRecordsCommand command) {
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

        PagedUsageRecords page = usageRecordRepository.query(filter, pageRequest);
        return PagedUsageRecordViews.of(toViews(page.getRecords(), true), page);
    }

    private List<UsageRecordView> toViews(List<UsageRecord> records, boolean adminView) {
        return records.stream()
                .map(record -> UsageRecordView.of(
                        record,
                        username(record),
                        apiCredentialName(record),
                        adminView ? providerChannelName(record) : null
                ))
                .toList();
    }

    private String username(UsageRecord record) {
        if (record.getUserAccountId() == null) {
            return null;
        }
        return userAccountRepository.findById(record.getUserAccountId())
                .map(user -> user.getUsername().getValue())
                .orElse(null);
    }

    private String apiCredentialName(UsageRecord record) {
        if (record.getApiCredentialId() == null) {
            return null;
        }
        return apiCredentialRepository.findById(record.getApiCredentialId())
                .map(credential -> credential.getName().getValue())
                .orElse(null);
    }

    private String providerChannelName(UsageRecord record) {
        if (record.getProviderChannelId() == null) {
            return null;
        }
        return providerChannelRepository.findById(record.getProviderChannelId())
                .map(ProviderChannel::name)
                .map(Object::toString)
                .orElse(null);
    }
}
