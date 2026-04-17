package com.wms.repository;

import com.wms.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByStatusAndVisibleOrderBySortNoAsc(String status, Boolean visible);
}
