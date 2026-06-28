package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId
    ) {
        return ApiResponse.ok(stockService.listStocks(keyword, warehouseId));
    }

    @GetMapping("/{id}/movements")
    public ApiResponse<?> movements(@PathVariable Long id) {
        return ApiResponse.ok(stockService.listMovements(id));
    }
}
