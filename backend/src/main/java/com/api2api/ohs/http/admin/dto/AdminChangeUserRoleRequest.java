package com.api2api.ohs.http.admin.dto;

import com.api2api.domain.user.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin change user role request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminChangeUserRoleRequest {

    @NotNull(message = "New role must not be null")
    private UserRole newRole;
}
