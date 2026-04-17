package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.ExceptionTicketRequest;
import com.wms.entity.ExceptionTicket;
import com.wms.entity.Warehouse;
import com.wms.repository.ExceptionTicketRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExceptionService {

    private final ExceptionTicketRepository exceptionTicketRepository;
    private final WarehouseRepository warehouseRepository;

    public List<Map<String, Object>> list() {
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        return exceptionTicketRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ticket -> toView(ticket, warehouseMap))
                .toList();
    }

    @Transactional
    public Map<String, Object> create(ExceptionTicketRequest request) {
        ExceptionTicket ticket = buildNewTicket(
                request.getWarehouseId(),
                request.getBizType() == null || request.getBizType().isBlank() ? "MANUAL" : request.getBizType(),
                request.getBizId(),
                request.getBizNo(),
                request.getTicketTitle(),
                request.getTicketContent(),
                request.getSeverity() == null || request.getSeverity().isBlank() ? "MEDIUM" : request.getSeverity(),
                request.getReporterName() == null || request.getReporterName().isBlank() ? "System Admin" : request.getReporterName()
        );
        ticket.setAssigneeName(request.getAssigneeName());
        ExceptionTicket saved = exceptionTicketRepository.save(ticket);
        return toView(saved, loadWarehouseMap());
    }

    @Transactional
    public Map<String, Object> assign(Long id, String assigneeName) {
        ExceptionTicket ticket = exceptionTicketRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Exception ticket not found"));
        ticket.setAssigneeName(assigneeName);
        ticket.setStatus("IN_PROGRESS");
        return toView(exceptionTicketRepository.save(ticket), loadWarehouseMap());
    }

    @Transactional
    public Map<String, Object> resolve(Long id, String operatorName) {
        ExceptionTicket ticket = exceptionTicketRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Exception ticket not found"));
        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(LocalDateTime.now());
        if (ticket.getAssigneeName() == null || ticket.getAssigneeName().isBlank()) {
            ticket.setAssigneeName(operatorName);
        }
        return toView(exceptionTicketRepository.save(ticket), loadWarehouseMap());
    }

    @Transactional
    public Map<String, Object> close(Long id) {
        ExceptionTicket ticket = exceptionTicketRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Exception ticket not found"));
        ticket.setStatus("CLOSED");
        ticket.setClosedAt(LocalDateTime.now());
        return toView(exceptionTicketRepository.save(ticket), loadWarehouseMap());
    }

    @Transactional
    public ExceptionTicket createFromStockTake(Long warehouseId, Long bizId, String bizNo, String title, String content, String severity, String reporterName) {
        return createOrRefreshSystemTicket(warehouseId, "STOCK_TAKE_ORDER", bizId, bizNo, title, content, severity, reporterName);
    }

    @Transactional
    public ExceptionTicket createOrRefreshSystemTicket(
            Long warehouseId,
            String bizType,
            Long bizId,
            String bizNo,
            String title,
            String content,
            String severity,
            String reporterName
    ) {
        return exceptionTicketRepository
                .findFirstByBizTypeAndBizIdAndStatusInOrderByIdDesc(bizType, bizId, List.of("OPEN", "IN_PROGRESS"))
                .map(existing -> {
                    existing.setTicketTitle(title);
                    existing.setTicketContent(content);
                    existing.setSeverity(severity);
                    return exceptionTicketRepository.save(existing);
                })
                .orElseGet(() -> exceptionTicketRepository.save(buildNewTicket(
                        warehouseId,
                        bizType,
                        bizId,
                        bizNo,
                        title,
                        content,
                        severity,
                        reporterName
                )));
    }

    private ExceptionTicket buildNewTicket(
            Long warehouseId,
            String bizType,
            Long bizId,
            String bizNo,
            String title,
            String content,
            String severity,
            String reporterName
    ) {
        ExceptionTicket ticket = new ExceptionTicket();
        ticket.setTicketNo("EX" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        ticket.setWarehouseId(warehouseId);
        ticket.setBizType(bizType == null || bizType.isBlank() ? "MANUAL" : bizType);
        ticket.setBizId(bizId);
        ticket.setBizNo(bizNo);
        ticket.setTicketTitle(title);
        ticket.setTicketContent(content);
        ticket.setSeverity(severity == null || severity.isBlank() ? "MEDIUM" : severity);
        ticket.setStatus("OPEN");
        ticket.setReporterName(reporterName == null || reporterName.isBlank() ? "System Admin" : reporterName);
        ticket.setApprovalStatus("NOT_SUBMITTED");
        ticket.setReportedAt(LocalDateTime.now());
        return ticket;
    }

    private Map<Long, Warehouse> loadWarehouseMap() {
        return warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
    }

    private Map<String, Object> toView(ExceptionTicket ticket, Map<Long, Warehouse> warehouseMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", ticket.getId());
        item.put("ticketNo", ticket.getTicketNo());
        item.put("warehouseId", ticket.getWarehouseId());
        item.put("warehouseName", ticket.getWarehouseId() == null || warehouseMap.get(ticket.getWarehouseId()) == null
                ? null
                : warehouseMap.get(ticket.getWarehouseId()).getWarehouseName());
        item.put("bizType", ticket.getBizType());
        item.put("bizId", ticket.getBizId());
        item.put("bizNo", ticket.getBizNo());
        item.put("ticketTitle", ticket.getTicketTitle());
        item.put("ticketContent", ticket.getTicketContent());
        item.put("severity", ticket.getSeverity());
        item.put("status", ticket.getStatus());
        item.put("assigneeName", ticket.getAssigneeName());
        item.put("reporterName", ticket.getReporterName());
        item.put("approvalStatus", ticket.getApprovalStatus());
        item.put("reportedAt", ticket.getReportedAt());
        item.put("resolvedAt", ticket.getResolvedAt());
        item.put("closedAt", ticket.getClosedAt());
        return item;
    }
}
