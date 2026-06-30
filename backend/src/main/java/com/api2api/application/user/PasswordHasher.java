package com.api2api.application.user;

import com.api2api.domain.user.model.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash passwordHash);
}
