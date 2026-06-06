package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inbound_order_item")
public class InboundOrderItem extends BaseEntity {

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

    @Column(name = "cargo_code")
    private String cargoCode;

    @Column(name = "cargo_code_type")
    private String cargoCodeType;

    @Column(name = "cargo_code_content", length = 2048)
    private String cargoCodeContent;

    @Column(name = "external_platform")
    private String externalPlatform;

    @Column(name = "external_code")
    private String externalCode;

    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "expected_qty", nullable = false)
    private BigDecimal expectedQty;

    @Column(name = "actual_qty", nullable = false)
    private BigDecimal actualQty;

    @Column(name = "qualified_qty", nullable = false)
    private BigDecimal qualifiedQty;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "putaway_scan_confirmed", nullable = false)
    private Boolean putawayScanConfirmed = false;

    @Column(name = "putaway_scan_confirmed_at")
    private LocalDateTime putawayScanConfirmedAt;

    @Column(name = "putaway_scan_operator")
    private String putawayScanOperator;

    @Column(name = "putaway_scan_record_id")
    private Long putawayScanRecordId;

    private String remark;
}
