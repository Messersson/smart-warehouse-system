package com.wms.repository;

import com.wms.entity.StockTakeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTakeItemRepository extends JpaRepository<StockTakeItem, Long> {

    List<StockTakeItem> findByTakeOrderIdOrderByIdAsc(Long takeOrderId);
}
