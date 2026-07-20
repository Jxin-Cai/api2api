package com.api2api.ohs.http.credential.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModelGroupListResponse {
    List<ModelGroupResponse> groups;
}
