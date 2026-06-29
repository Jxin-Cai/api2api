package com.api2api.ohs.http.usage.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usage record page response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordPageResponse {

    private List<UsageRecordResponse> records;
    private int page;
    private int size;
    private long totalElements;
    private long totalPages;
    private long filteredTotalTokens;
    private boolean adminView;
}
