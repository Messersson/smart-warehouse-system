package com.wms.repository;

import com.wms.entity.BillingContract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingContractRepository extends JpaRepository<BillingContract, Long> {
}
