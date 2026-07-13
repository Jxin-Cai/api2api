package com.api2api.infr.repository.credential;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.repository.credential.converter.ApiCredentialPersistenceConverter;
import com.api2api.infr.repository.credential.mapper.ApiCredentialMapper;
import com.api2api.infr.repository.credential.po.ApiCredentialPO;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ApiCredentialRepositoryImpl implements ApiCredentialRepository {

    @NonNull
    private final ApiCredentialMapper mapper;

    @NonNull
    private final ApiCredentialPersistenceConverter converter;

    @Override
    public void save(ApiCredential apiCredential) {
        Objects.requireNonNull(apiCredential, "ApiCredential must not be null");
        ApiCredentialPO po = converter.toPO(apiCredential);
        if (mapper.selectById(po.getId()) == null) {
            mapper.insert(po);
        } else {
            mapper.update(po);
        }
    }

    @Override
    public Optional<ApiCredential> findById(ApiCredentialId id) {
        Objects.requireNonNull(id, "ApiCredentialId must not be null");
        return Optional.ofNullable(mapper.selectById(id.value()))
                .map(converter::toDomain);
    }

    @Override
    public Optional<ApiCredential> findByKeyHash(ApiKeyHash keyHash) {
        Objects.requireNonNull(keyHash, "ApiKeyHash must not be null");
        return Optional.ofNullable(mapper.selectByKeyHash(keyHash.getValue()))
                .map(converter::toDomain);
    }

    @Override
    public List<ApiCredential> findByOwnerUserId(UserAccountId ownerUserId) {
        Objects.requireNonNull(ownerUserId, "Owner user id must not be null");
        return mapper.selectByOwnerUserId(ownerUserId.getValue()).stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public void softDeleteById(ApiCredentialId id, Instant deletedAt) {
        Objects.requireNonNull(id, "ApiCredentialId must not be null");
        Objects.requireNonNull(deletedAt, "Deleted time must not be null");
        mapper.softDeleteById(id.value(), deletedAt);
    }
}
