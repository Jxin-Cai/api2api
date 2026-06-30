package com.api2api.application.user;

import com.api2api.application.BusinessException;
import com.api2api.application.user.command.ChangeUserDisplayNameCommand;
import com.api2api.application.user.command.ChangeUserRoleCommand;
import com.api2api.application.user.command.ChangeUserStatusCommand;
import com.api2api.application.user.command.CreateUserCommand;
import com.api2api.application.user.command.LoginCommand;
import com.api2api.application.user.command.UpdateCurrentUserProfileCommand;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.PasswordHash;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserAccountStatus;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final PasswordHasher passwordHasher;

    @NonNull
    private final Clock clock;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public UserAccount login(LoginCommand command) {
        UserAccount userAccount = userAccountRepository.findByUsername(command.getUsername())
                .orElseThrow(this::invalidCredentials);
        if (userAccount.getStatus() != UserAccountStatus.ACTIVE) {
            throw invalidCredentials();
        }
        if (!userAccount.hasPasswordHash() || !passwordHasher.matches(command.getPassword(), userAccount.getPasswordHash())) {
            throw invalidCredentials();
        }
        return userAccount;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public UserAccount getCurrentUser(UserAccountId currentUserId) {
        UserAccount userAccount = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        userAccount.assertActive();
        return userAccount;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAccount updateCurrentUserProfile(UpdateCurrentUserProfileCommand command) {
        UserAccount currentUser = userAccountRepository.findById(command.getCurrentUserId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        currentUser.assertActive();
        currentUser.changeDisplayName(command.getDisplayName(), now());
        userAccountRepository.save(currentUser);
        return currentUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAccount createUser(CreateUserCommand command) {
        loadAdminOperator(command.getOperatorUserId());
        userAccountRepository.findByUsername(command.getUsername())
                .ifPresent(existing -> {
                    throw new BusinessException("USERNAME_ALREADY_EXISTS");
                });

        PasswordHash passwordHash = passwordHasher.hash(command.getPassword());
        UserAccount userAccount = UserAccount.create(
                command.getUserAccountId(),
                command.getUsername(),
                command.getDisplayName(),
                command.getRole(),
                passwordHash,
                now()
        );
        userAccountRepository.save(userAccount);
        return userAccount;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAccount changeDisplayName(ChangeUserDisplayNameCommand command) {
        loadAdminOperator(command.getOperatorUserId());
        UserAccount target = loadTargetUser(command.getTargetUserId());
        target.changeDisplayName(command.getDisplayName(), now());
        userAccountRepository.save(target);
        return target;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAccount changeRole(ChangeUserRoleCommand command) {
        loadAdminOperator(command.getOperatorUserId());
        UserAccount target = loadTargetUser(command.getTargetUserId());
        target.changeRole(command.getNewRole(), now());
        userAccountRepository.save(target);
        return target;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAccount disableUser(ChangeUserStatusCommand command) {
        loadAdminOperator(command.getOperatorUserId());
        UserAccount target = loadTargetUser(command.getTargetUserId());
        target.disable(now());
        userAccountRepository.save(target);
        return target;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAccount enableUser(ChangeUserStatusCommand command) {
        loadAdminOperator(command.getOperatorUserId());
        UserAccount target = loadTargetUser(command.getTargetUserId());
        target.enable(now());
        userAccountRepository.save(target);
        return target;
    }

    private UserAccount loadAdminOperator(UserAccountId operatorUserId) {
        UserAccount operator = userAccountRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
        return operator;
    }

    private UserAccount loadTargetUser(UserAccountId targetUserId) {
        return userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS");
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
