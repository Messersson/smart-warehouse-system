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
@Table(name = "scan_record")
public class ScanRecord extends BaseEntity {

    @Lob
    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "parsed_code")
    private String parsedCode;

    @Column(name = "scan_format")
    private String scanFormat;

    @Column(name = "content_format")
    private String contentFormat;

    @Column(name = "code_type")
    private String codeType;

    @Column(name = "merchant_platform")
    private String merchantPlatform;

    @Column(name = "matched", nullable = false)
    private Boolean matched;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "source_device")
    private String sourceDevice;

    @Column(name = "scanner_interface")
    private String scannerInterface;

    @Column(name = "scanner_device_id")
    private String scannerDeviceId;

    @Column(name = "operator_name")
    private String operatorName;

    private String remark;
}
