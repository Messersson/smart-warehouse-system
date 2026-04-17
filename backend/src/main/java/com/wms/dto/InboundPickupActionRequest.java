package com.wms.dto;

import lombok.Data;

@Data
public class InboundPickupActionRequest {

    private String action;
    private String operatorName;
    private Integer extendMinutes;
    private String remark;
}
