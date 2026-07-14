package com.api2api.ohs.http.admin;

import com.api2api.application.protocol.ProtocolConversionApplicationService;
import com.api2api.application.protocol.command.ChangeProtocolConversionStatusCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.admin.converter.ProtocolConversionHttpConverter;
import com.api2api.ohs.http.admin.dto.ProtocolConversionListResponse;
import com.api2api.ohs.http.admin.dto.ProtocolConversionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for protocol conversion definitions.
 */
@RestController
@RequestMapping("/api/admin/protocol-conversions")
@Validated
@RequiredArgsConstructor
public class AdminProtocolConversionController {

    @NonNull
    private final ProtocolConversionApplicationService protocolConversionApplicationService;

    @NonNull
    private final ProtocolConversionHttpConverter protocolConversionHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @GetMapping
    public ApiResponse<ProtocolConversionListResponse> listDefinitions(HttpServletRequest request) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        List<ProtocolConversionDefinition> definitions = protocolConversionApplicationService.listDefinitions(operatorUserId);
        return ApiResponse.success(protocolConversionHttpConverter.toListResponse(definitions));
    }

    @GetMapping("/{definition-id}")
    public ApiResponse<ProtocolConversionResponse> getDefinition(
            @PathVariable("definition-id") Long definitionId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ProtocolConversionDefinitionId id = ProtocolConversionDefinitionId.of(definitionId);
        ProtocolConversionDefinition definition = protocolConversionApplicationService.getDefinition(operatorUserId, id);
        return ApiResponse.success(protocolConversionHttpConverter.toResponse(definition));
    }

    @GetMapping("/by-direction")
    public ApiResponse<ProtocolConversionResponse> getDefinitionByDirection(
            @RequestParam("source-protocol") String sourceProtocol,
            @RequestParam("target-protocol") String targetProtocol,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ProtocolType source = protocolConversionHttpConverter.toProtocolType(sourceProtocol);
        ProtocolType target = protocolConversionHttpConverter.toProtocolType(targetProtocol);
        ProtocolConversionDefinition definition = protocolConversionApplicationService.getDefinitionByDirection(
                operatorUserId, source, target);
        return ApiResponse.success(protocolConversionHttpConverter.toResponse(definition));
    }

    @PatchMapping("/{definition-id}/enable")
    public ApiResponse<ProtocolConversionResponse> enableConversion(
            @PathVariable("definition-id") Long definitionId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ProtocolConversionDefinitionId id = ProtocolConversionDefinitionId.of(definitionId);
        ChangeProtocolConversionStatusCommand command = protocolConversionHttpConverter.toChangeStatusCommand(
                operatorUserId, id);
        ProtocolConversionDefinition definition = protocolConversionApplicationService.enableConversion(command);
        return ApiResponse.success(protocolConversionHttpConverter.toResponse(definition));
    }

    @PatchMapping("/{definition-id}/disable")
    public ApiResponse<ProtocolConversionResponse> disableConversion(
            @PathVariable("definition-id") Long definitionId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        ProtocolConversionDefinitionId id = ProtocolConversionDefinitionId.of(definitionId);
        ChangeProtocolConversionStatusCommand command = protocolConversionHttpConverter.toChangeStatusCommand(
                operatorUserId, id);
        ProtocolConversionDefinition definition = protocolConversionApplicationService.disableConversion(command);
        return ApiResponse.success(protocolConversionHttpConverter.toResponse(definition));
    }
}
