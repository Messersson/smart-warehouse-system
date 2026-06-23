package com.wms.repository;

import com.wms.entity.CargoCodeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CargoCodeRecordRepository extends JpaRepository<CargoCodeRecord, Long> {

    List<CargoCodeRecord> findByInboundOrderItemIdOrderByIdAsc(Long inboundOrderItemId);

    Optional<CargoCodeRecord> findFirstByCargoCodeOrderByIdDesc(String cargoCode);

    Optional<CargoCodeRecord> findFirstByRawContentOrderByIdDesc(String rawContent);

    List<CargoCodeRecord> findAllByOrderByCreatedAtDesc();
}
