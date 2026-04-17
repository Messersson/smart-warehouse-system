package com.wms.repository;

import com.wms.entity.ApprovalOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalOrderRepository extends JpaRepository<ApprovalOrder, Long> {

    List<ApprovalOrder> findAllByOrderByCreatedAtDesc();

    Optional<ApprovalOrder> findFirstByBizTypeAndBizIdOrderByCreatedAtDesc(String bizType, Long bizId);
}
