package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalSubmitRequest {

    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String approvalType;
    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String bizType;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long bizId;
    private String bizNo;
    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String applicantName;
    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String approverName;
    private String applyReason;
}
