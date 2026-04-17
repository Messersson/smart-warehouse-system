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
@Table(name = "inventory_stock")
public class Stock extends BaseEntity {

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "locked_qty", nullable = false)
    private BigDecimal lockedQty;

    @Column(name = "available_qty", nullable = false)
    private BigDecimal availableQty;

    @Column(name = "last_inbound_at")
    private LocalDateTime lastInboundAt;

    @Column(name = "last_outbound_at")
    private LocalDateTime lastOutboundAt;

    @Column(name = "last_movement_at")
    private LocalDateTime lastMovementAt;
}
