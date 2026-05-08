package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.StockTakeCountRequest;
import com.wms.dto.StockTakeCreateRequest;
import com.wms.service.StockTakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-takes")
@RequiredArgsConstructor
public class StockTakeController {

    private final StockTakeService stockTakeService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(stockTakeService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(stockTakeService.detail(id));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody StockTakeCreateRequest request) {
        return ApiResponse.ok(stockTakeService.create(request));
    }

    @PostMapping("/{id}/count")
    public ApiResponse<?> count(@PathVariable Long id, @Valid @RequestBody StockTakeCountRequest request) {
        return ApiResponse.ok(stockTakeService.count(id, request));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<?> finish(@PathVariable Long id) {
        return ApiResponse.ok(stockTakeService.finish(id));
    }

    @PostMapping("/{id}/adjust")
    public ApiResponse<?> adjust(@PathVariable Long id) {
        return ApiResponse.ok(stockTakeService.adjust(id));
    }
}
