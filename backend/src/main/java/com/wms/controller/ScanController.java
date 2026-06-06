package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.CodeRenderRequest;
import com.wms.dto.ScanRecordRequest;
import com.wms.service.CodeRenderService;
import com.wms.service.CargoCodeRecordService;
import com.wms.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;
    private final CodeRenderService codeRenderService;
    private final CargoCodeRecordService cargoCodeRecordService;

    @GetMapping("/lookup")
    public ApiResponse<?> lookup(@RequestParam String code) {
        return ApiResponse.ok(scanService.lookup(code));
    }

    @GetMapping("/records")
    public ApiResponse<?> records(@RequestParam(defaultValue = "50") Integer limit) {
        return ApiResponse.ok(scanService.listRecords(limit));
    }

    @PostMapping("/records")
    public ApiResponse<?> saveRecord(@RequestBody ScanRecordRequest request) {
        return ApiResponse.ok(scanService.saveRecord(request));
    }

    @PostMapping("/render-code")
    public ApiResponse<?> renderCode(@RequestBody CodeRenderRequest request) {
        return ApiResponse.ok(codeRenderService.render(request));
    }

    @GetMapping("/cargo-code-records")
    public ApiResponse<?> cargoCodeRecords(@RequestParam(defaultValue = "100") Integer limit) {
        return ApiResponse.ok(cargoCodeRecordService.list(limit));
    }

    @GetMapping("/cargo-code-records/{id}")
    public ApiResponse<?> cargoCodeRecord(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return ApiResponse.ok(cargoCodeRecordService.detail(id));
    }
}
