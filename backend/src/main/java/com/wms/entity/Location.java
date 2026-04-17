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
@Table(name = "base_location")
public class Location extends BaseEntity {

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "location_code", nullable = false)
    private String locationCode;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(name = "aisle_no")
    private String aisleNo;

    @Column(name = "shelf_no")
    private String shelfNo;

    @Column(name = "layer_no")
    private String layerNo;

    @Column(name = "bin_no")
    private String binNo;

    @Column(name = "capacity_qty", nullable = false)
    private BigDecimal capacityQty;

    @Column(name = "used_qty", nullable = false)
    private BigDecimal usedQty;

    @Column(nullable = false)
    private String status;

    private Boolean pickable;

    private String remark;
}
