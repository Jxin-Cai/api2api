package com.api2api.ohs.http.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token amount response including raw tokens and millions unit.
 * Shared by front and admin dashboard responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenAmountResponse {

    private long tokens;
    private double millions;
}
