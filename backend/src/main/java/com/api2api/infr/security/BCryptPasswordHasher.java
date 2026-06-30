package com.api2api.infr.security;

import com.api2api.application.user.PasswordHasher;
import com.api2api.domain.user.model.PasswordHash;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(String rawPassword) {
        String requiredPassword = requireRawPassword(rawPassword);
        return PasswordHash.of(encoder.encode(requiredPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        String requiredPassword = requireRawPassword(rawPassword);
        return encoder.matches(requiredPassword, passwordHash.getValue());
    }

    private static String requireRawPassword(String rawPassword) {
        String requiredPassword = Objects.requireNonNull(rawPassword, "Password must not be null");
        if (requiredPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return requiredPassword;
    }
}
