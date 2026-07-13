package com.api2api.infr.repository.credential.mapper;

import com.api2api.infr.repository.credential.po.ApiCredentialPO;
import java.time.Instant;
import java.util.List;

public interface ApiCredentialMapper {
    int insert(ApiCredentialPO apiCredential);
    int update(ApiCredentialPO apiCredential);
    ApiCredentialPO selectById(Long id);
    ApiCredentialPO selectByKeyHash(String keyHash);
    List<ApiCredentialPO> selectByOwnerUserId(Long ownerUserId);
    int softDeleteById(Long id, Instant updatedAt);
}
