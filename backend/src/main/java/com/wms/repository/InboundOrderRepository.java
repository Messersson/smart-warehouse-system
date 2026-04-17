package com.wms.repository;

import com.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    List<InboundOrder> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    List<InboundOrder> findByPickupStatusInOrderByCreatedAtDesc(List<String> pickupStatuses);

    Optional<InboundOrder> findFirstByOrderNoOrPickupCodeOrScanCodeOrderByIdDesc(String orderNo, String pickupCode, String scanCode);
}
