package com.api2api.ohs.http.credential;

import com.api2api.application.credential.ModelGroupApplicationService;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.credential.converter.ModelGroupHttpConverter;
import com.api2api.ohs.http.credential.dto.ModelGroupListResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Portal users can choose centrally managed groups but cannot change their policies. */
@RestController
@RequestMapping("/api/model-groups")
@RequiredArgsConstructor
public class ModelGroupOptionsController {
    private final ModelGroupApplicationService service;
    private final ModelGroupHttpConverter converter;
    private final CurrentUserContextResolver users;

    @GetMapping
    public ApiResponse<ModelGroupListResponse> list(HttpServletRequest request) {
        return ApiResponse.success(converter.toListResponse(service.listMyGroupViews(users.resolveCurrentUserId(request))));
    }
}
