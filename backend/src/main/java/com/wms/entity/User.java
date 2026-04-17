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
@Table(name = "sys_user")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String phone;

    private String email;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    @Column(nullable = false)
    private String status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    private String remark;
}
