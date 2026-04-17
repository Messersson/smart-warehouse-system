package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.BillingStatementGenerateRequest;
import com.wms.entity.BillingContract;
import com.wms.entity.BillingRule;
import com.wms.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @RequestMapping(value = "/overview", method = RequestMethod.GET)
    public ApiResponse<?> overview() {
        return ApiResponse.ok(billingService.overview());
    }

    @PostMapping("/contracts")
    public ApiResponse<?> createContract(@RequestBody BillingContract contract) {
        return ApiResponse.ok(billingService.saveContract(contract));
    }

    @PutMapping("/contracts/{id}")
    public ApiResponse<?> updateContract(@PathVariable Long id, @RequestBody BillingContract contract) {
        contract.setId(id);
        return ApiResponse.ok(billingService.saveContract(contract));
    }

    @PostMapping("/rules")
    public ApiResponse<?> createRule(@RequestBody BillingRule rule) {
        return ApiResponse.ok(billingService.saveRule(rule));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<?> updateRule(@PathVariable Long id, @RequestBody BillingRule rule) {
        rule.setId(id);
        return ApiResponse.ok(billingService.saveRule(rule));
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<?> deleteRule(@PathVariable Long id) {
        billingService.deleteRule(id);
        return ApiResponse.ok("deleted", null);
    }

    @PostMapping("/statements/generate")
    public ApiResponse<?> generate(@RequestBody BillingStatementGenerateRequest request) {
        return ApiResponse.ok(billingService.generateStatement(request));
    }

    @PostMapping("/statements/{id}/pay")
    public ApiResponse<?> markPaid(@PathVariable Long id) {
        return ApiResponse.ok(billingService.markPaid(id));
    }
}
