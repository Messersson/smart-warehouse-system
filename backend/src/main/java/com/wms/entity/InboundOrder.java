package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inbound_order")
public class InboundOrder extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(nullable = false)
    private String status;

    @Column(name = "source_no")
    private String sourceNo;

    @Column(name = "expected_arrival_time")
    private LocalDateTime expectedArrivalTime;

    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "putaway_completed_at")
    private LocalDateTime putawayCompletedAt;

    @Column(name = "total_expected_qty", nullable = false)
    private BigDecimal totalExpectedQty;

    @Column(name = "total_actual_qty", nullable = false)
    private BigDecimal totalActualQty;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "pickup_status", nullable = false)
    private String pickupStatus;

    @Column(name = "pickup_code")
    private String pickupCode;

    @Column(name = "pickup_due_at")
    private LocalDateTime pickupDueAt;

    @Column(name = "last_sms_notice_at")
    private LocalDateTime lastSmsNoticeAt;

    @Column(name = "scan_code")
    private String scanCode;

    private String remark;
}
