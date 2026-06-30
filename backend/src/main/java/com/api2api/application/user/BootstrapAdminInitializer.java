package com.api2api.application.user;

import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserRole;
import com.api2api.domain.user.model.Username;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final DisplayName DEFAULT_ADMIN_DISPLAY_NAME = DisplayName.of("Admin");

    @NonNull
    private final BootstrapAdminProperties properties;

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final PasswordHasher passwordHasher;

    @NonNull
    private final Clock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        Username username = Username.of(requireUsername());
        Instant now = Instant.now(clock);

        userAccountRepository.findByUsername(username)
                .ifPresentOrElse(
                        admin -> prepareExistingAdmin(admin, now),
                        () -> createBootstrapAdmin(username, now)
                );
    }

    private void prepareExistingAdmin(UserAccount admin, Instant now) {
        if (admin.hasPasswordHash()) {
            return;
        }

        admin.ensureAdminActive(now);
        admin.changePasswordHash(passwordHasher.hash(requirePassword()), now);
        userAccountRepository.save(admin);
    }

    private void createBootstrapAdmin(Username username, Instant now) {
        UserAccount admin = UserAccount.create(
                userAccountRepository.nextIdentity(),
                username,
                DEFAULT_ADMIN_DISPLAY_NAME,
                UserRole.ADMIN,
                passwordHasher.hash(requirePassword()),
                now
        );
        userAccountRepository.save(admin);
    }

    private String requireUsername() {
        String username = properties.username();
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("ADMIN_USERNAME must not be blank");
        }
        return username;
    }

    private String requirePassword() {
        String password = properties.password();
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD must not be blank");
        }
        return password;
    }
}
