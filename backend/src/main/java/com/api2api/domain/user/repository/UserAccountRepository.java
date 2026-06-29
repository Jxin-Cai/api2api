package com.api2api.domain.user.repository;

import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.Username;

import java.util.Optional;

public interface UserAccountRepository {

    /**
     * Saves a complete user account aggregate.
     * Implementations should create or update by id, preserve aggregate invariants, and guarantee username uniqueness.
     * A duplicate username owned by another account or any persistence failure should be reported as a business failure.
     *
     * @param userAccount complete user account aggregate to save
     */
    void save(UserAccount userAccount);

    /**
     * Loads a complete user account aggregate by id, including basic profile, role and status fields.
     * Returns {@link Optional#empty()} when no account exists for the id.
     * Invalid ids should be rejected by the {@link UserAccountId} value object before repository access.
     *
     * @param id user account id
     * @return optional complete user account aggregate
     */
    Optional<UserAccount> findById(UserAccountId id);

    /**
     * Loads a complete user account aggregate by username for login, authorization context loading and uniqueness checks.
     * Returns {@link Optional#empty()} when no account exists for the username.
     * Invalid usernames should be rejected by the {@link Username} value object before repository access.
     *
     * @param username login username
     * @return optional complete user account aggregate
     */
    Optional<UserAccount> findByUsername(Username username);
}
