package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "base_customer")
public class Customer extends BaseEntity {

    @Column(name = "customer_code", nullable = false, unique = true)
    private String customerCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_type", nullable = false)
    private String customerType;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String email;

    private String address;

    @Column(nullable = false)
    private String status;

    private String remark;
}
