package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundOrderRequest {

    private String orderNo;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long warehouseId;
    private Long supplierId;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
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
    @Valid
    @NotEmpty(message = "\u81f3\u5c11\u9700\u8981\u4e00\u4e2a\u660e\u7ec6")
    private List<InboundOrderItemRequest> items;
}
