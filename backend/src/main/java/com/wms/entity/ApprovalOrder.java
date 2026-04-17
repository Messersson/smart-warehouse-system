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
@Table(name = "approval_order")
public class ApprovalOrder extends BaseEntity {

    @Column(name = "approval_no", nullable = false, unique = true)
    private String approvalNo;

    @Column(name = "approval_type", nullable = false)
    private String approvalType;

    @Column(name = "biz_type", nullable = false)
    private String bizType;

    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    @Column(name = "biz_no")
    private String bizNo;

    @Column(name = "applicant_name")
    private String applicantName;

    @Column(name = "approver_name")
    private String approverName;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_node")
    private String currentNode;

    @Column(name = "apply_reason")
    private String applyReason;

    @Column(name = "approval_comment")
    private String approvalComment;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
