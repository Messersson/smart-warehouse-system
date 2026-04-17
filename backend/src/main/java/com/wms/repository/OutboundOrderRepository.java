package com.wms.repository;

import com.wms.entity.OutboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {

    List<OutboundOrder> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    Optional<OutboundOrder> findFirstByOrderNoOrLogisticsNoOrderByIdDesc(String orderNo, String logisticsNo);
}
