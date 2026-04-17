package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "base_owner")
public class Owner extends BaseEntity {

    @Column(name = "owner_code", nullable = false, unique = true)
    private String ownerCode;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "owner_type", nullable = false)
    private String ownerType;

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
