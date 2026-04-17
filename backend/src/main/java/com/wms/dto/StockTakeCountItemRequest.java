package com.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockTakeCountItemRequest {

    private Long id;
    private BigDecimal actualQty;
    private String remark;
}
