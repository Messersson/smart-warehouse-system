package com.wms.repository;

import com.wms.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByWarehouseIdOrderByLocationCodeAsc(Long warehouseId);

    List<Location> findByWarehouseIdAndStatusOrderByLocationCodeAsc(Long warehouseId, String status);

    java.util.Optional<Location> findFirstByLocationCodeOrderByIdAsc(String locationCode);
}
