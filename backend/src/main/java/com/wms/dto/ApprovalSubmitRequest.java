package com.wms.dto;

import lombok.Data;

@Data
public class ApprovalSubmitRequest {

    private String approvalType;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String applicantName;
    private String approverName;
    private String applyReason;
}
