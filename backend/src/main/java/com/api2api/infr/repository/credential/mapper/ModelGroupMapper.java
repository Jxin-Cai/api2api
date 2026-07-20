package com.api2api.infr.repository.credential.mapper;

import com.api2api.infr.repository.credential.po.ModelGroupPO;
import java.time.Instant;
import java.util.List;

public interface ModelGroupMapper {
    int insert(ModelGroupPO modelGroup);
    int update(ModelGroupPO modelGroup);
    ModelGroupPO selectById(Long id);
    List<ModelGroupPO> selectByOwnerUserId(Long ownerUserId);
    boolean existsByOwnerAndNameExcludingId(Long ownerUserId, String name, Long excludedId);
    boolean existsCredentialBinding(Long id);
    int softDeleteById(Long id, Instant updatedAt);
}
