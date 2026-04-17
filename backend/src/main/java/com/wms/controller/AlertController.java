package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(alertService.listAlerts());
    }

    @PostMapping("/scan")
    public ApiResponse<?> scanNow() {
        alertService.scanAlerts();
        return ApiResponse.ok("scan triggered", null);
    }

    @PostMapping("/{id}/ack")
    public ApiResponse<?> acknowledge(@PathVariable Long id, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok("acknowledged", alertService.acknowledge(id, operatorName));
    }
}
