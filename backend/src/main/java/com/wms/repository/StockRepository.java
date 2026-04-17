package com.wms.repository;

import com.wms.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findAllByOrderByUpdatedAtDesc();

    List<Stock> findByWarehouseIdAndOwnerIdAndProductIdOrderByUpdatedAtDesc(Long warehouseId, Long ownerId, Long productId);

    List<Stock> findByWarehouseIdAndProductIdOrderByUpdatedAtDesc(Long warehouseId, Long productId);

    List<Stock> findByWarehouseIdAndOwnerIdAndProductIdAndQuantityGreaterThanOrderByLastMovementAtAsc(
            Long warehouseId,
            Long ownerId,
            Long productId,
            BigDecimal quantity
    );

    List<Stock> findByWarehouseIdAndProductIdAndQuantityGreaterThanOrderByLastMovementAtAsc(
            Long warehouseId,
            Long productId,
            BigDecimal quantity
    );
}
