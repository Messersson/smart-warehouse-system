package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "alert_event")
public class AlertEvent extends BaseEntity {

    @Column(name = "alert_code", nullable = false)
    private String alertCode;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "biz_type", nullable = false)
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(nullable = false)
    private String severity;

    @Column(name = "alert_message", nullable = false)
    private String alertMessage;

    @Column(nullable = false)
    private String status;

    @Column(name = "first_triggered_at", nullable = false)
    private LocalDateTime firstTriggeredAt;

    @Column(name = "last_triggered_at", nullable = false)
    private LocalDateTime lastTriggeredAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
