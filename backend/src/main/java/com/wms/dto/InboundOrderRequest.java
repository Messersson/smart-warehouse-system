package com.wms.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundOrderRequest {

    private String orderNo;
    private Long warehouseId;
    private Long supplierId;
    private Long ownerId;
    private Long customerId;
    private String orderType;
    private String sourceNo;
    private LocalDateTime expectedArrivalTime;
    private String operatorName;
    private String receiverName;
    private String receiverPhone;
    private String pickupCode;
    private String scanCode;
    private String remark;
    private List<InboundOrderItemRequest> items;
}
