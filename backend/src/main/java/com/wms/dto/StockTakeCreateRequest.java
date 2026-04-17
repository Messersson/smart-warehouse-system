package com.wms.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockTakeCreateRequest {

    private Long warehouseId;
    private Long ownerId;
    private String takeType;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private String operatorName;
    private String remark;
}
