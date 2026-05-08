package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StockTakeCountRequest {

    @Valid
    @NotEmpty(message = "\u81f3\u5c11\u9700\u8981\u4e00\u4e2a\u76d8\u70b9\u660e\u7ec6")
    private List<StockTakeCountItemRequest> items;
}
