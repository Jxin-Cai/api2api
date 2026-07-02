package com.api2api.ohs.http.channel;

import com.api2api.application.channel.ProviderChannelApplicationService;
import com.api2api.application.channel.dto.ProviderModelOption;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.channel.dto.ProviderModelOptionListResponse;
import com.api2api.ohs.http.channel.dto.ProviderModelOptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class ProviderModelController {

    @NonNull
    private final ProviderChannelApplicationService providerChannelApplicationService;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @GetMapping("/api/provider-models")
    public ApiResponse<ProviderModelOptionListResponse> listProviderModels(HttpServletRequest request) {
        UserAccountId userId = currentUserContextResolver.resolveCurrentUserId(request);
        List<ProviderModelOptionResponse> models = providerChannelApplicationService.listProviderModelOptions(userId).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(ProviderModelOptionListResponse.builder().models(models).build());
    }

    private ProviderModelOptionResponse toResponse(ProviderModelOption option) {
        return ProviderModelOptionResponse.builder()
                .model(option.model().value())
                .providerCount(option.providerCount())
                .protocols(option.protocols())
                .build();
    }
}
