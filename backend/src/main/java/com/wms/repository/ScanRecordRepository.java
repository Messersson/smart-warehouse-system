package com.wms.repository;

import com.wms.entity.ScanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScanRecordRepository extends JpaRepository<ScanRecord, Long> {

    List<ScanRecord> findAllByOrderByCreatedAtDesc();
}
