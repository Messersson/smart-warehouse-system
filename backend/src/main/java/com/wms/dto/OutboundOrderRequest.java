package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundOrderRequest {

    private String orderNo;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long warehouseId;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long customerId;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long ownerId;
    private Long carrierId;
    private String orderType;
    private String sourceNo;
    private String priorityLevel;
    private LocalDateTime plannedShipTime;
    private String operatorName;
    private String logisticsNo;
    private String remark;
    @Valid
    @NotEmpty(message = "\u81f3\u5c11\u9700\u8981\u4e00\u4e2a\u660e\u7ec6")
    private List<OutboundOrderItemRequest> items;
}
