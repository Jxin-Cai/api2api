package com.api2api.ohs.http.admin.dto;

import com.api2api.domain.user.model.UserAccountStatus;
import com.api2api.domain.user.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * User account response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountResponse {

    private Long id;
    private String username;
    private String displayName;
    private UserRole role;
    private UserAccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
