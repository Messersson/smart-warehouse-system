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
@Table(name = "billing_statement_item")
public class BillingStatementItem extends BaseEntity {

    @Column(name = "statement_id", nullable = false)
    private Long statementId;

    @Column(name = "charge_type", nullable = false)
    private String chargeType;

    @Column(name = "charge_name", nullable = false)
    private String chargeName;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "biz_no")
    private String bizNo;
}
