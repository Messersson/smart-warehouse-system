package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "inventory_movement")
public class InventoryMovement extends BaseEntity {

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

    @Column(name = "movement_type", nullable = false)
    private String movementType;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_no")
    private String sourceNo;

    @Column(name = "before_qty", nullable = false)
    private BigDecimal beforeQty;

    @Column(name = "change_qty", nullable = false)
    private BigDecimal changeQty;

    @Column(name = "after_qty", nullable = false)
    private BigDecimal afterQty;

    @Column(name = "operator_name")
    private String operatorName;

    private String remark;
}
