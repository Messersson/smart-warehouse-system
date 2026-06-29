package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.VirtualPlatformOrderRequest;
import com.wms.service.VirtualPlatformService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/virtual-platform")
@RequiredArgsConstructor
public class VirtualPlatformController {

    private final VirtualPlatformService virtualPlatformService;

    @PostMapping("/orders")
    public ApiResponse<?> createOrder(@Valid @RequestBody VirtualPlatformOrderRequest request) {
        return ApiResponse.ok("virtual platform supply order synced", virtualPlatformService.createInboundOrder(request));
    }
}
