package com.api2api.infr.repository.user.po;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persistence object mapped to the user_accounts table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountPO {

    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String status;
    private Instant createdTime;
    private Instant updatedTime;
    private boolean deleted;
}
