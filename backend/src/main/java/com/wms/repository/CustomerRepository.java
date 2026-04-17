package com.wms.repository;

import com.wms.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    java.util.Optional<Customer> findByCustomerCode(String customerCode);
}
