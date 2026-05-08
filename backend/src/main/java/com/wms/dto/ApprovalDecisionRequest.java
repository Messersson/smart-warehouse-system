package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalDecisionRequest {

    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String operatorName;
    private String comment;
}
