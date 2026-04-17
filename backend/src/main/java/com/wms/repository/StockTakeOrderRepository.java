package com.wms.repository;

import com.wms.entity.StockTakeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTakeOrderRepository extends JpaRepository<StockTakeOrder, Long> {

    List<StockTakeOrder> findAllByOrderByCreatedAtDesc();
}
