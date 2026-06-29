package com.api2api.application.credential;

import com.api2api.application.BusinessException;
import com.api2api.application.credential.command.AuthenticateApiCredentialCommand;
import com.api2api.application.credential.command.ChangeApiCredentialStatusCommand;
import com.api2api.application.credential.command.ChangeTokenLimitCommand;
import com.api2api.application.credential.command.CreateApiCredentialCommand;
import com.api2api.application.credential.command.RenameApiCredentialCommand;
import com.api2api.application.credential.command.ReplaceModelWhitelistCommand;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.usage.repository.UsageRecordRepository;
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
public class ApiCredentialApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final ApiCredentialRepository apiCredentialRepository;

    @NonNull
    private final UsageRecordRepository usageRecordRepository;

    @NonNull
    private final Clock clock;

    @Transactional(rollbackFor = Exception.class)
    public ApiCredential createCredential(CreateApiCredentialCommand command) {
        assertUserPortal(command.getOwnerUserId());

        ApiCredential apiCredential = ApiCredential.create(
                command.getApiCredentialId(),
                command.getOwnerUserId(),
                command.getName(),
                command.getKeyHash(),
                command.getKeyPreview(),
                command.getModelWhitelist(),
                command.getTokenLimit(),
                now()
        );
        apiCredentialRepository.save(apiCredential);
        return apiCredential;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ApiCredential> listMyCredentials(UserAccountId ownerUserId) {
        assertUserPortal(ownerUserId);
        return apiCredentialRepository.findByOwnerUserId(ownerUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredential renameCredential(RenameApiCredentialCommand command) {
        assertUserPortal(command.getOwnerUserId());
        ApiCredential apiCredential = loadCredential(command.getApiCredentialId());

        apiCredential.assertOwnedBy(command.getOwnerUserId());
        apiCredential.rename(command.getName(), now());
        apiCredentialRepository.save(apiCredential);
        return apiCredential;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredential replaceModelWhitelist(ReplaceModelWhitelistCommand command) {
        assertUserPortal(command.getOwnerUserId());
        ApiCredential apiCredential = loadCredential(command.getApiCredentialId());

        apiCredential.assertOwnedBy(command.getOwnerUserId());
        apiCredential.replaceModelWhitelist(command.getModelWhitelist(), now());
        apiCredentialRepository.save(apiCredential);
        return apiCredential;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredential changeTokenLimit(ChangeTokenLimitCommand command) {
        assertUserPortal(command.getOwnerUserId());
        ApiCredential apiCredential = loadCredential(command.getApiCredentialId());

        apiCredential.assertOwnedBy(command.getOwnerUserId());
        long currentConsumedTokens = currentConsumedTokens(command.getApiCredentialId());
        apiCredential.changeTokenLimit(command.getTokenLimit(), currentConsumedTokens, now());
        apiCredentialRepository.save(apiCredential);
        return apiCredential;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredential disableCredential(ChangeApiCredentialStatusCommand command) {
        assertUserPortal(command.getOwnerUserId());
        ApiCredential apiCredential = loadCredential(command.getApiCredentialId());

        apiCredential.assertOwnedBy(command.getOwnerUserId());
        apiCredential.disable(now());
        apiCredentialRepository.save(apiCredential);
        return apiCredential;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredential enableCredential(ChangeApiCredentialStatusCommand command) {
        assertUserPortal(command.getOwnerUserId());
        ApiCredential apiCredential = loadCredential(command.getApiCredentialId());

        apiCredential.assertOwnedBy(command.getOwnerUserId());
        apiCredential.enable(now());
        apiCredentialRepository.save(apiCredential);
        return apiCredential;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredential authenticateForGateway(AuthenticateApiCredentialCommand command) {
        ApiCredential apiCredential = apiCredentialRepository.findByKeyHash(command.getKeyHash())
                .orElseThrow(() -> new BusinessException("API_CREDENTIAL_INVALID"));

        apiCredential.assertUsable();
        apiCredential.assertModelAllowed(command.getRequestedModel());
        long currentConsumedTokens = currentConsumedTokens(apiCredential.getId());
        apiCredential.assertQuotaAvailable(currentConsumedTokens);
        apiCredential.markUsed(now());
        apiCredentialRepository.save(apiCredential);
        return apiCredential;
    }

    private void assertUserPortal(UserAccountId ownerUserId) {
        UserAccount user = userAccountRepository.findById(ownerUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        user.assertCanAccess(AccessScope.USER_PORTAL);
    }

    private ApiCredential loadCredential(ApiCredentialId apiCredentialId) {
        return apiCredentialRepository.findById(apiCredentialId)
                .orElseThrow(() -> new BusinessException("API_CREDENTIAL_NOT_FOUND"));
    }

    private long currentConsumedTokens(ApiCredentialId apiCredentialId) {
        long currentConsumedTokens = usageRecordRepository.sumTotalTokensByApiCredential(apiCredentialId);
        if (currentConsumedTokens < 0) {
            throw new BusinessException("INVALID_TOKEN_TOTAL");
        }
        return currentConsumedTokens;
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
