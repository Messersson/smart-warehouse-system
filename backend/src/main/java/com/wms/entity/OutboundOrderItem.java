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
@Table(name = "outbound_order_item")
public class OutboundOrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "planned_qty", nullable = false)
    private BigDecimal plannedQty;

    @Column(name = "shipped_qty", nullable = false)
    private BigDecimal shippedQty;

    @Column(name = "location_id")
    private Long locationId;

    private String remark;
}
