package com.wms.repository;

import com.wms.entity.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    List<ApprovalRecord> findByApprovalOrderIdOrderByCreatedAtAsc(Long approvalOrderId);
}
