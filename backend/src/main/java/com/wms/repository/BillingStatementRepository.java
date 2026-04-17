package com.wms.repository;

import com.wms.entity.BillingStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingStatementRepository extends JpaRepository<BillingStatement, Long> {

    List<BillingStatement> findAllByOrderByCreatedAtDesc();
}
