package com.wms.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundOrderRequest {

    private String orderNo;
    private Long warehouseId;
    private Long customerId;
    private Long ownerId;
    private Long carrierId;
    private String orderType;
    private String sourceNo;
    private String priorityLevel;
    private LocalDateTime plannedShipTime;
    private String operatorName;
    private String logisticsNo;
    private String remark;
    private List<OutboundOrderItemRequest> items;
}
