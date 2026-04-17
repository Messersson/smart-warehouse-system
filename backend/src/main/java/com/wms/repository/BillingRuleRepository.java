package com.wms.repository;

import com.wms.entity.BillingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingRuleRepository extends JpaRepository<BillingRule, Long> {

    List<BillingRule> findByContractIdOrderByIdAsc(Long contractId);
}
