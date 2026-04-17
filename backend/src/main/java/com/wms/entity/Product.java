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
@Table(name = "base_product")
public class Product extends BaseEntity {

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_spec")
    private String productSpec;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "unit_name", nullable = false)
    private String unitName;

    private String barcode;

    @Column(name = "safe_stock", nullable = false)
    private BigDecimal safeStock;

    @Column(name = "max_stock", nullable = false)
    private BigDecimal maxStock;

    @Column(name = "shelf_life_days", nullable = false)
    private Integer shelfLifeDays;

    @Column(name = "enable_batch", nullable = false)
    private Boolean enableBatch;

    @Column(name = "enable_serial", nullable = false)
    private Boolean enableSerial;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", nullable = false)
    private BigDecimal volumeM3;

    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private String status;

    private String remark;
}
