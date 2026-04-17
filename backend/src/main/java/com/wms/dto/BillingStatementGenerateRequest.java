package com.wms.dto;

import lombok.Data;

@Data
public class BillingStatementGenerateRequest {

    private Long contractId;
    private String statementMonth;
}
