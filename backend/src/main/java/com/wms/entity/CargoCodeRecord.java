package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cargo_code_record")
public class CargoCodeRecord extends BaseEntity {

    @Column(name = "inbound_order_id", nullable = false)
    private Long inboundOrderId;

    @Column(name = "inbound_order_item_id", nullable = false)
    private Long inboundOrderItemId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "operation_type")
    private String operationType;

    @Column(name = "operation_code")
    private String operationCode;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "location_code")
    private String locationCode;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "cargo_code", nullable = false)
    private String cargoCode;

    @Column(name = "cargo_code_type", nullable = false)
    private String cargoCodeType;

    @Lob
    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Lob
    @Column(name = "svg_content", columnDefinition = "MEDIUMTEXT")
    private String svgContent;

    @Column(name = "render_format")
    private String renderFormat;

    @Column(name = "render_width")
    private Integer renderWidth;

    @Column(name = "render_height")
    private Integer renderHeight;

    @Column(name = "external_platform")
    private String externalPlatform;

    @Column(name = "external_code")
    private String externalCode;

    @Column(nullable = false)
    private String status;

    private String remark;
}
