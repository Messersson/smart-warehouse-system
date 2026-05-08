package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InboundPickupActionRequest {

    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String action;
    private String operatorName;
    private Integer extendMinutes;
    private String remark;
}
