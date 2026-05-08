package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.ApprovalDecisionRequest;
import com.wms.dto.ApprovalSubmitRequest;
import com.wms.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(approvalService.list());
    }

    @PostMapping("/submit")
    public ApiResponse<?> submit(@Valid @RequestBody ApprovalSubmitRequest request) {
        return ApiResponse.ok(approvalService.submit(request));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<?> approve(@PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
        return ApiResponse.ok(approvalService.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<?> reject(@PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
        return ApiResponse.ok(approvalService.reject(id, request));
    }
}
