package com.wms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InboundOrderItemRequest {

    private Long productId;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private BigDecimal qualifiedQty;
    private Long locationId;
    private String remark;
}
