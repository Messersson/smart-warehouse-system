package com.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VirtualPlatformOrderItemRequest {

    private Long productId;
    private String skuCode;
    private String barcode;
    private String platformSku;
    @DecimalMin(value = "0.0001", message = "\u5fc5\u987b\u5927\u4e8e0")
    private BigDecimal quantity;
    private String batchNo;
    private Long locationId;
    private String cargoCodeType;
    private String externalCode;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private String remark;
}
