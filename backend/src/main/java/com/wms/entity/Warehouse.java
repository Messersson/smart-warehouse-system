package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "base_warehouse")
public class Warehouse extends BaseEntity {

    @Column(name = "warehouse_code", nullable = false, unique = true)
    private String warehouseCode;

    @Column(name = "warehouse_name", nullable = false)
    private String warehouseName;

    @Column(name = "warehouse_type", nullable = false)
    private String warehouseType;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String province;

    private String city;

    private String district;

    private String address;

    @Column(nullable = false)
    private String status;

    @Column(name = "scene_type", nullable = false)
    private String sceneType;

    @Column(name = "dwell_alert_minutes", nullable = false)
    private Integer dwellAlertMinutes;

    @Column(name = "sms_notify_enabled", nullable = false)
    private Boolean smsNotifyEnabled;

    @Column(name = "sms_reminder_interval_minutes", nullable = false)
    private Integer smsReminderIntervalMinutes;

    @Column(name = "auto_assign_location", nullable = false)
    private Boolean autoAssignLocation;

    @Column(name = "scan_mode", nullable = false)
    private String scanMode;

    private String remark;
}
