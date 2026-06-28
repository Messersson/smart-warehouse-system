package com.wms.repository;

import com.wms.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    List<AlertEvent> findAllByOrderByLastTriggeredAtDesc();

    long countByStatus(String status);

    Optional<AlertEvent> findFirstByAlertCodeAndStatusInOrderByIdDesc(String alertCode, List<String> statuses);

    long deleteByLastTriggeredAtBefore(LocalDateTime cutoffTime);
}
