package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BillingStatementGenerateRequest {

    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long contractId;
    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "\u683c\u5f0f\u5e94\u4e3a yyyy-MM")
    private String statementMonth;
}
