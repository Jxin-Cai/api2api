package com.api2api.ohs.http.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User account list response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountListResponse {

    private List<UserAccountResponse> users;
}
