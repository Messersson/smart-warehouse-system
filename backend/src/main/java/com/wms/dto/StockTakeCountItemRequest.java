package com.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockTakeCountItemRequest {

    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long id;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    @DecimalMin(value = "0", message = "\u4e0d\u80fd\u5c0f\u4e8e0")
    private BigDecimal actualQty;
    private String remark;
}
