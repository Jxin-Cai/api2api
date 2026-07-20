package com.api2api.ohs.http.credential.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModelGroupResponse {
    Long id;
    String name;
    List<String> modelWhitelist;
    Instant createdAt;
    Instant updatedAt;
}
