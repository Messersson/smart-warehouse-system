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
@Table(name = "billing_rule")
public class BillingRule extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "charge_type", nullable = false)
    private String chargeType;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "unit_name", nullable = false)
    private String unitName;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "step_json")
    private String stepJson;

    @Column(nullable = false)
    private String status;
}
