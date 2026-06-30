package com.api2api.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.PasswordHash;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserAccountStatus;
import com.api2api.domain.user.model.UserRole;
import com.api2api.domain.user.model.Username;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;

class BootstrapAdminInitializerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void preservesExistingAdminPasswordHash() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        UserAccount existingAdmin = adminWithPasswordHash(PasswordHash.of("existing-password-hash"));
        when(repository.findByUsername(Username.of("admin"))).thenReturn(Optional.of(existingAdmin));

        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
                new BootstrapAdminProperties("admin", ""),
                repository,
                passwordHasher,
                CLOCK
        );

        initializer.run(null);

        verify(repository, never()).save(any());
        verifyNoInteractions(passwordHasher);
        assertThat(existingAdmin.getPasswordHash()).isEqualTo(PasswordHash.of("existing-password-hash"));
    }

    @Test
    void initializesMissingAdminPasswordHashOnce() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        UserAccount existingAdmin = adminWithPasswordHash(null);
        PasswordHash bootstrapPasswordHash = PasswordHash.of("bootstrap-password-hash");
        when(repository.findByUsername(Username.of("admin"))).thenReturn(Optional.of(existingAdmin));
        when(passwordHasher.hash("admin123")).thenReturn(bootstrapPasswordHash);

        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
                new BootstrapAdminProperties("admin", "admin123"),
                repository,
                passwordHasher,
                CLOCK
        );

        initializer.run(null);

        ArgumentCaptor<UserAccount> savedAdmin = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(savedAdmin.capture());
        assertThat(savedAdmin.getValue().getPasswordHash()).isEqualTo(bootstrapPasswordHash);
        assertThat(savedAdmin.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(savedAdmin.getValue().getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
    }

    private static UserAccount adminWithPasswordHash(PasswordHash passwordHash) {
        return UserAccount.rehydrate(
                UserAccountId.of(1L),
                Username.of("admin"),
                DisplayName.of("Admin"),
                UserRole.ADMIN,
                UserAccountStatus.ACTIVE,
                passwordHash,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-01T00:00:00Z")
        );
    }
}
