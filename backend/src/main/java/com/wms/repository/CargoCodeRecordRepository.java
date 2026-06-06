package com.wms.repository;

import com.wms.entity.CargoCodeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CargoCodeRecordRepository extends JpaRepository<CargoCodeRecord, Long> {

    List<CargoCodeRecord> findByInboundOrderItemIdOrderByIdAsc(Long inboundOrderItemId);

    List<CargoCodeRecord> findAllByOrderByCreatedAtDesc();
}
