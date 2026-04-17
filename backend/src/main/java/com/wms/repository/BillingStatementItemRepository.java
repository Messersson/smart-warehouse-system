package com.wms.repository;

import com.wms.entity.BillingStatementItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingStatementItemRepository extends JpaRepository<BillingStatementItem, Long> {

    List<BillingStatementItem> findByStatementIdOrderByIdAsc(Long statementId);
}
