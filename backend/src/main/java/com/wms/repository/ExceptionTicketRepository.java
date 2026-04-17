package com.wms.repository;

import com.wms.entity.ExceptionTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExceptionTicketRepository extends JpaRepository<ExceptionTicket, Long> {

    List<ExceptionTicket> findAllByOrderByCreatedAtDesc();

    Optional<ExceptionTicket> findFirstByBizTypeAndBizIdAndStatusInOrderByIdDesc(String bizType, Long bizId, List<String> statuses);
}
