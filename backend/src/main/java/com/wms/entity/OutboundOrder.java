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
@Table(name = "outbound_order")
public class OutboundOrder extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(nullable = false)
    private String status;

    @Column(name = "source_no")
    private String sourceNo;

    @Column(name = "priority_level", nullable = false)
    private String priorityLevel;

    @Column(name = "planned_ship_time")
    private LocalDateTime plannedShipTime;

    @Column(name = "picking_completed_at")
    private LocalDateTime pickingCompletedAt;

    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "total_planned_qty", nullable = false)
    private BigDecimal totalPlannedQty;

    @Column(name = "total_shipped_qty", nullable = false)
    private BigDecimal totalShippedQty;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "logistics_no")
    private String logisticsNo;

    private String remark;
}
