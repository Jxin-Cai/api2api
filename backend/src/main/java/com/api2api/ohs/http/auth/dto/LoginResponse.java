package com.api2api.ohs.http.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response body for MVP login.
 */
@Data
@Builder
public class LoginResponse {

    private Long currentUserId;
    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String status;
}
