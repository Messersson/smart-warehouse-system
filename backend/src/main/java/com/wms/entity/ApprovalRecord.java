package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_record")
public class ApprovalRecord extends BaseEntity {

    @Column(name = "approval_order_id", nullable = false)
    private Long approvalOrderId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "action_comment")
    private String actionComment;
}
