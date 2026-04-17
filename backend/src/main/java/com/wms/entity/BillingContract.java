package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "billing_contract")
public class BillingContract extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "contract_no", nullable = false, unique = true)
    private String contractNo;

    @Column(name = "contract_name", nullable = false)
    private String contractName;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(name = "settlement_cycle", nullable = false)
    private String settlementCycle;

    @Column(nullable = false)
    private String status;
}
