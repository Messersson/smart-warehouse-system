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
@Table(name = "stock_take_item")
public class StockTakeItem extends BaseEntity {

    @Column(name = "take_order_id", nullable = false)
    private Long takeOrderId;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "system_qty", nullable = false)
    private BigDecimal systemQty;

    @Column(name = "actual_qty", nullable = false)
    private BigDecimal actualQty;

    @Column(name = "diff_qty", nullable = false)
    private BigDecimal diffQty;

    @Column(nullable = false)
    private String status;

    private String remark;
}
