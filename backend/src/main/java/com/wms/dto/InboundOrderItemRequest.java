package com.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InboundOrderItemRequest {

    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long productId;
    private String batchNo;
    private String cargoCodeType;
    private String cargoCode;
    private String externalPlatform;
    private String externalCode;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    @DecimalMin(value = "0.0001", message = "\u5fc5\u987b\u5927\u4e8e0")
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private BigDecimal qualifiedQty;
    private Long locationId;
    private String remark;
}
