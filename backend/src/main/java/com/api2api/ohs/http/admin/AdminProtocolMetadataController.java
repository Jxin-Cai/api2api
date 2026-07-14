package com.api2api.ohs.http.admin;

import com.api2api.application.protocolmetadata.ProtocolMetadataApplicationService;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.admin.converter.ProtocolMetadataHttpConverter;
import com.api2api.ohs.http.admin.dto.ProtocolMetadataDetailResponse;
import com.api2api.ohs.http.admin.dto.ProtocolMetadataListResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/protocol-metadata")
@Validated
@RequiredArgsConstructor
public class AdminProtocolMetadataController {

    @NonNull
    private final ProtocolMetadataApplicationService protocolMetadataApplicationService;

    @NonNull
    private final ProtocolMetadataHttpConverter protocolMetadataHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @GetMapping
    public ApiResponse<ProtocolMetadataListResponse> listProtocolMetadata(HttpServletRequest request) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        List<ProtocolMetadata> metadataList = protocolMetadataApplicationService.listAll(operatorUserId);
        return ApiResponse.success(protocolMetadataHttpConverter.toListResponse(metadataList));
    }

    @GetMapping("/{protocol-type}")
    public ApiResponse<ProtocolMetadataDetailResponse> getProtocolMetadataDetail(
            @PathVariable("protocol-type") String protocolType,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ProtocolType type = ProtocolType.valueOf(protocolType.toUpperCase().replace('-', '_'));
        ProtocolMetadata metadata = protocolMetadataApplicationService.getByProtocolType(operatorUserId, type);
        return ApiResponse.success(protocolMetadataHttpConverter.toDetailResponse(metadata));
    }
}
