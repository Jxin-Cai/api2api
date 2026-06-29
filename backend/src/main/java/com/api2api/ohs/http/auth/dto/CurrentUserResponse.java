package com.api2api.ohs.http.auth.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * Response body for current user profile.
 */
@Data
@Builder
public class CurrentUserResponse {

    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
