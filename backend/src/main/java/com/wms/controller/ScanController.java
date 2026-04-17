package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @GetMapping("/lookup")
    public ApiResponse<?> lookup(@RequestParam String code) {
        return ApiResponse.ok(scanService.lookup(code));
    }
}
