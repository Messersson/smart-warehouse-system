package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VirtualPlatformOrderRequest {

    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String platformType;
    @NotBlank(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private String platformOrderNo;
    private String shopName;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long warehouseId;
    private Long supplierId;
    private Long customerId;
    @NotNull(message = "\u4e0d\u80fd\u4e3a\u7a7a")
    private Long ownerId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String logisticsNo;
    private String priorityLevel;
    private LocalDateTime plannedShipTime;
    private LocalDateTime expectedArrivalTime;
    private String operatorName;
    private String remark;
    @Valid
    @NotEmpty(message = "\u81f3\u5c11\u9700\u8981\u4e00\u4e2a\u660e\u7ec6")
    private List<VirtualPlatformOrderItemRequest> items;
}
