package com.api2api.domain.credential.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.user.model.UserAccountId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelGroupTest {

    @Test
    void test_updates_whitelist_when_group_configuration_changes() {
        // Arrange
        Instant createdAt = Instant.parse("2026-07-20T00:00:00Z");
        ModelGroup group = ModelGroup.create(
                ModelGroupId.of(1L),
                UserAccountId.of(2L),
                ModelGroupName.of("production"),
                ModelWhitelist.of(Set.of(ModelName.of("gpt-4.1"))),
                createdAt
        );
        ModelWhitelist changedWhitelist = ModelWhitelist.of(Set.of(ModelName.of("claude-sonnet-4")));

        // Act
        group.update(ModelGroupName.of("production"), changedWhitelist, createdAt.plusSeconds(60));

        // Assert
        assertThat(group.getModelWhitelist()).isEqualTo(changedWhitelist);
    }
}
