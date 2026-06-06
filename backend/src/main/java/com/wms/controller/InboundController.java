package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.InboundOrderRequest;
import com.wms.dto.InboundPickupActionRequest;
import com.wms.dto.InboundPutawayScanRequest;
import com.wms.service.InboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inbounds")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService inboundService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(inboundService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(inboundService.detail(id));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody InboundOrderRequest request) {
        return ApiResponse.ok(inboundService.create(request));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<?> receive(@PathVariable Long id) {
        return ApiResponse.ok("receive completed", inboundService.receive(id));
    }

    @PostMapping("/{id}/putaway")
    public ApiResponse<?> putaway(@PathVariable Long id) {
        return ApiResponse.ok("putaway completed", inboundService.putaway(id));
    }

    @PostMapping("/scan-putaway")
    public ApiResponse<?> scanPutaway(@RequestBody InboundPutawayScanRequest request) {
        return ApiResponse.ok("scan putaway confirmed", inboundService.scanPutaway(request));
    }

    @PostMapping("/{id}/pickup-action")
    public ApiResponse<?> pickupAction(@PathVariable Long id, @Valid @RequestBody InboundPickupActionRequest request) {
        return ApiResponse.ok(inboundService.handlePickupAction(id, request));
    }
}
