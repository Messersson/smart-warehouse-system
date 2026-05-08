package com.wms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockTakeCreateRequest {

    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long warehouseId;
    private Long ownerId;
    private String takeType;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private String operatorName;
    private String remark;
}
