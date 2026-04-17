package com.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutboundOrderItemRequest {

    private Long productId;
    private String batchNo;
    private BigDecimal plannedQty;
    private BigDecimal shippedQty;
    private Long locationId;
    private String remark;
}
