package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "alert_rule")
public class AlertRule extends BaseEntity {

    @Column(name = "rule_code", nullable = false, unique = true)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(name = "biz_type", nullable = false)
    private String bizType;

    @Column(name = "threshold_value", nullable = false)
    private Integer thresholdValue;

    @Column(name = "threshold_unit", nullable = false)
    private String thresholdUnit;

    @Column(nullable = false)
    private String severity;

    @Column(name = "notify_to")
    private String notifyTo;

    @Column(nullable = false)
    private Boolean enabled;

    private String remark;
}
