package com.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutboundOrderItemRequest {

    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long productId;
    private String batchNo;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    @DecimalMin(value = "0.0001", message = "\u5fc5\u987b\u5927\u4e8e0")
    private BigDecimal plannedQty;
    private BigDecimal shippedQty;
    private Long locationId;
    private String remark;
}
