package com.wms.repository;

import com.wms.entity.InboundOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InboundOrderItemRepository extends JpaRepository<InboundOrderItem, Long> {

    List<InboundOrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    List<InboundOrderItem> findByProductIdAndOutboundOrderIdIsNullOrderByIdDesc(Long productId);

    Optional<InboundOrderItem> findFirstByCargoCodeOrExternalCodeOrderByIdDesc(String cargoCode, String externalCode);

    Optional<InboundOrderItem> findFirstByCargoCodeContentOrderByIdDesc(String cargoCodeContent);
}
