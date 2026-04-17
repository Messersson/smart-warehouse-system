package com.wms.dto;

import lombok.Data;

@Data
public class ApprovalDecisionRequest {

    private String operatorName;
    private String comment;
}
