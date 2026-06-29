package com.api2api.domain.credential.repository;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.user.model.UserAccountId;

import java.util.List;
import java.util.Optional;

public interface ApiCredentialRepository {

    /**
     * Saves a complete API credential aggregate.
     * Implementations should create or update by id, preserve aggregate invariants, guarantee key hash uniqueness,
     * and never persist plaintext API key material. Duplicate key hashes, missing owners, or persistence failures
     * should be reported as business failures.
     *
     * @param apiCredential complete API credential aggregate to save
     */
    void save(ApiCredential apiCredential);

    /**
     * Loads a complete API credential aggregate by id, including owner, status, whitelist, token limit and display data.
     * Returns {@link Optional#empty()} when no API credential exists for the id.
     * Invalid ids should be rejected by the {@link ApiCredentialId} value object before repository access.
     *
     * @param id API credential id
     * @return optional complete API credential aggregate
     */
    Optional<ApiCredential> findById(ApiCredentialId id);

    /**
     * Loads a complete API credential aggregate by API key hash for gateway authentication.
     * Returns {@link Optional#empty()} when no API credential exists for the hash, and must never return plaintext key material.
     * Invalid hashes should be rejected by the {@link ApiKeyHash} value object before repository access.
     *
     * @param keyHash API key hash or fingerprint
     * @return optional complete API credential aggregate
     */
    Optional<ApiCredential> findByKeyHash(ApiKeyHash keyHash);

    /**
     * Loads all API credential aggregates owned by one user for personal key management.
     * Implementations must only return aggregates whose owner equals the given user id and provide stable created-at descending order.
     * Invalid owner ids should be rejected by the {@link UserAccountId} value object before repository access.
     *
     * @param ownerUserId owner user id
     * @return API credentials owned by the user in stable created-at descending order
     */
    List<ApiCredential> findByOwnerUserId(UserAccountId ownerUserId);
}
