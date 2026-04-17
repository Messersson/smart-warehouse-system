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
@Table(name = "billing_statement")
public class BillingStatement extends BaseEntity {

    @Column(name = "statement_no", nullable = false, unique = true)
    private String statementNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "statement_month", nullable = false)
    private String statementMonth;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;

    @Column(name = "statement_status", nullable = false)
    private String statementStatus;
}
