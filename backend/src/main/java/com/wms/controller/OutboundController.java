package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.OutboundOrderRequest;
import com.wms.service.OutboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outbounds")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundService outboundService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(outboundService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(outboundService.detail(id));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody OutboundOrderRequest request) {
        return ApiResponse.ok(outboundService.create(request));
    }

    @PostMapping("/{id}/picking")
    public ApiResponse<?> picking(@PathVariable Long id) {
        return ApiResponse.ok("拣货完成", outboundService.picking(id));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<?> ship(@PathVariable Long id) {
        return ApiResponse.ok("发运完成", outboundService.ship(id));
    }
}
