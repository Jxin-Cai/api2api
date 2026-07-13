package com.api2api.ohs.http.dashboard.dto;

import java.math.BigDecimal;
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

    private BigDecimal tokens;
    private double millions;
}
