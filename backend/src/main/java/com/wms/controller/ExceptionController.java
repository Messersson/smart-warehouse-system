package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.ExceptionTicketRequest;
import com.wms.service.ExceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exceptions")
@RequiredArgsConstructor
public class ExceptionController {

    private final ExceptionService exceptionService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(exceptionService.list());
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody ExceptionTicketRequest request) {
        return ApiResponse.ok(exceptionService.create(request));
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<?> assign(@PathVariable Long id, @RequestParam String assigneeName) {
        return ApiResponse.ok(exceptionService.assign(id, assigneeName));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<?> resolve(@PathVariable Long id, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(exceptionService.resolve(id, operatorName));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<?> close(@PathVariable Long id) {
        return ApiResponse.ok(exceptionService.close(id));
    }
}
