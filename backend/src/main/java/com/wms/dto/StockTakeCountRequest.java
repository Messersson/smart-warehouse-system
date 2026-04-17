package com.wms.dto;

import lombok.Data;

import java.util.List;

@Data
public class StockTakeCountRequest {

    private List<StockTakeCountItemRequest> items;
}
