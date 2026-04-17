package com.wms.repository;

import com.wms.entity.OutboundOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboundOrderItemRepository extends JpaRepository<OutboundOrderItem, Long> {

    List<OutboundOrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}
