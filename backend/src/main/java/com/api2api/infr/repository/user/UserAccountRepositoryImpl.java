package com.api2api.infr.repository.user;

import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.Username;
import com.api2api.domain.user.repository.UserAccountRepository;
import com.api2api.infr.repository.user.converter.UserAccountPersistenceConverter;
import com.api2api.infr.repository.user.mapper.UserAccountMapper;
import com.api2api.infr.repository.user.po.UserAccountPO;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Infrastructure implementation of {@link UserAccountRepository}.
 */
@Repository
@RequiredArgsConstructor
public class UserAccountRepositoryImpl implements UserAccountRepository {

    @NonNull
    private final UserAccountMapper mapper;

    @NonNull
    private final UserAccountPersistenceConverter converter;

    @Override
    public UserAccountId nextIdentity() {
        long timestampPart = System.currentTimeMillis() * 1_000L;
        long randomPart = ThreadLocalRandom.current().nextLong(1_000L);
        return UserAccountId.of(timestampPart + randomPart);
    }

    @Override
    public void save(UserAccount userAccount) {
        UserAccountPO po = converter.toPO(Objects.requireNonNull(userAccount, "User account must not be null"));
        if (mapper.selectById(po.getId()) == null) {
            mapper.insert(po);
            return;
        }
        mapper.update(po);
    }

    @Override
    public Optional<UserAccount> findById(UserAccountId id) {
        Objects.requireNonNull(id, "User account id must not be null");
        return Optional.ofNullable(mapper.selectById(id.getValue()))
                .filter(po -> !po.isDeleted())
                .map(converter::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(Username username) {
        Objects.requireNonNull(username, "Username must not be null");
        return Optional.ofNullable(mapper.selectByUsername(username.getValue()))
                .filter(po -> !po.isDeleted())
                .map(converter::toDomain);
    }

    @Override
    public List<UserAccount> findAll() {
        return mapper.selectAll().stream()
                .filter(po -> !po.isDeleted())
                .map(converter::toDomain)
                .toList();
    }
}
