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
@Table(name = "stock_take_order")
public class StockTakeOrder extends BaseEntity {

    @Column(name = "take_no", nullable = false, unique = true)
    private String takeNo;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "take_type", nullable = false)
    private String takeType;

    @Column(nullable = false)
    private String status;

    @Column(name = "planned_start_time")
    private LocalDateTime plannedStartTime;

    @Column(name = "planned_end_time")
    private LocalDateTime plannedEndTime;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "operator_name")
    private String operatorName;

    private String remark;
}
