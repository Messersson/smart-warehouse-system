package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "exception_ticket")
public class ExceptionTicket extends BaseEntity {

    @Column(name = "ticket_no", nullable = false, unique = true)
    private String ticketNo;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "biz_type", nullable = false)
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(name = "biz_no")
    private String bizNo;

    @Column(name = "ticket_title", nullable = false)
    private String ticketTitle;

    @Lob
    @Column(name = "ticket_content")
    private String ticketContent;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status;

    @Column(name = "assignee_name")
    private String assigneeName;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "approval_status", nullable = false)
    private String approvalStatus;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
