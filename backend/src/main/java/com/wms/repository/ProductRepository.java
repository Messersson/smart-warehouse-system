package com.wms.repository;

import com.wms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findFirstBySkuCodeOrBarcodeOrderByIdAsc(String skuCode, String barcode);

    Optional<Product> findFirstByBarcodeOrderByIdAsc(String barcode);
}
