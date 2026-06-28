package com.wms.repository;

import com.wms.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @Query("""
            select movement from InventoryMovement movement
            where movement.warehouseId = :warehouseId
              and ((:ownerId is null and movement.ownerId is null) or movement.ownerId = :ownerId)
              and movement.productId = :productId
              and ((:locationId is null and movement.locationId is null) or movement.locationId = :locationId)
              and ((:batchNo is null and (movement.batchNo is null or movement.batchNo = '')) or movement.batchNo = :batchNo)
            order by movement.createdAt desc, movement.id desc
            """)
    List<InventoryMovement> findStockMovements(
            @Param("warehouseId") Long warehouseId,
            @Param("ownerId") Long ownerId,
            @Param("productId") Long productId,
            @Param("locationId") Long locationId,
            @Param("batchNo") String batchNo
    );
}
