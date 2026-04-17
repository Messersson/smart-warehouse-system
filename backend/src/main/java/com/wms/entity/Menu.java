package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_menu")
public class Menu extends BaseEntity {

    @Column(name = "menu_code", nullable = false, unique = true)
    private String menuCode;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "menu_path")
    private String menuPath;

    @Column(name = "component_name")
    private String componentName;

    @Column(name = "icon_name")
    private String iconName;

    @Column(name = "menu_type", nullable = false)
    private String menuType;

    @Column(name = "permission_code")
    private String permissionCode;

    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    @Column(nullable = false)
    private Boolean visible;

    @Column(nullable = false)
    private String status;
}
