package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.ApprovalDecisionRequest;
import com.wms.dto.ApprovalSubmitRequest;
import com.wms.entity.ApprovalOrder;
import com.wms.entity.ApprovalRecord;
import com.wms.entity.ExceptionTicket;
import com.wms.repository.ApprovalOrderRepository;
import com.wms.repository.ApprovalRecordRepository;
import com.wms.repository.ExceptionTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalOrderRepository approvalOrderRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final ExceptionTicketRepository exceptionTicketRepository;

    @Value("${wms.approval.auto-approve:false}")
    private boolean autoApprove;

    public List<Map<String, Object>> list() {
        return approvalOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toView)
                .toList();
    }

    public String getLatestApprovalStatus(String bizType, Long bizId) {
        return approvalOrderRepository.findFirstByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId)
                .map(ApprovalOrder::getStatus)
                .orElse("NOT_SUBMITTED");
    }

    @Transactional
    public Map<String, Object> submit(ApprovalSubmitRequest request) {
        approvalOrderRepository.findFirstByBizTypeAndBizIdOrderByCreatedAtDesc(request.getBizType(), request.getBizId())
                .filter(order -> "PENDING".equals(order.getStatus()))
                .ifPresent(order -> {
                    throw new BusinessException("There is already a pending approval for this business document");
                });

        ApprovalOrder order = new ApprovalOrder();
        order.setApprovalNo("AP" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        order.setApprovalType(defaultText(request.getApprovalType(), "BUSINESS_APPROVAL"));
        order.setBizType(request.getBizType());
        order.setBizId(request.getBizId());
        order.setBizNo(request.getBizNo());
        order.setApplicantName(defaultText(request.getApplicantName(), "System Admin"));
        order.setApproverName(defaultText(request.getApproverName(), "Warehouse Manager"));
        order.setStatus("PENDING");
        order.setCurrentNode("MANAGER_REVIEW");
        order.setApplyReason(request.getApplyReason());
        order.setAppliedAt(LocalDateTime.now());
        if (autoApprove) {
            order.setStatus("APPROVED");
            order.setApprovalComment("自动审批通过");
            order.setDecidedAt(LocalDateTime.now());
        }
        ApprovalOrder saved = approvalOrderRepository.save(order);

        saveRecord(saved.getId(), "SUBMIT", saved.getApplicantName(), saved.getApplyReason());
        if (autoApprove) {
            saveRecord(saved.getId(), "AUTO_APPROVE", saved.getApproverName(), "自动审批通过");
        }
        updateLinkedBizApprovalStatus(saved, saved.getStatus());
        return toView(saved);
    }

    @Transactional
    public Map<String, Object> approve(Long id, ApprovalDecisionRequest request) {
        ApprovalOrder order = approvalOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Approval order not found"));
        order.setStatus("APPROVED");
        order.setApprovalComment(request.getComment());
        order.setDecidedAt(LocalDateTime.now());
        ApprovalOrder saved = approvalOrderRepository.save(order);
        saveRecord(saved.getId(), "APPROVE", defaultText(request.getOperatorName(), "Approver"), request.getComment());
        updateLinkedBizApprovalStatus(saved, "APPROVED");
        return toView(saved);
    }

    @Transactional
    public Map<String, Object> reject(Long id, ApprovalDecisionRequest request) {
        ApprovalOrder order = approvalOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Approval order not found"));
        order.setStatus("REJECTED");
        order.setApprovalComment(request.getComment());
        order.setDecidedAt(LocalDateTime.now());
        ApprovalOrder saved = approvalOrderRepository.save(order);
        saveRecord(saved.getId(), "REJECT", defaultText(request.getOperatorName(), "Approver"), request.getComment());
        updateLinkedBizApprovalStatus(saved, "REJECTED");
        return toView(saved);
    }

    private void updateLinkedBizApprovalStatus(ApprovalOrder approvalOrder, String status) {
        if ("EXCEPTION_TICKET".equals(approvalOrder.getBizType())) {
            exceptionTicketRepository.findById(approvalOrder.getBizId()).ifPresent(ticket -> {
                ticket.setApprovalStatus(status);
                exceptionTicketRepository.save(ticket);
            });
        }
    }

    private void saveRecord(Long approvalOrderId, String actionType, String operatorName, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setApprovalOrderId(approvalOrderId);
        record.setActionType(actionType);
        record.setOperatorName(operatorName);
        record.setActionComment(comment);
        approvalRecordRepository.save(record);
    }

    private Map<String, Object> toView(ApprovalOrder order) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("approvalNo", order.getApprovalNo());
        item.put("approvalType", order.getApprovalType());
        item.put("bizType", order.getBizType());
        item.put("bizId", order.getBizId());
        item.put("bizNo", order.getBizNo());
        item.put("applicantName", order.getApplicantName());
        item.put("approverName", order.getApproverName());
        item.put("status", order.getStatus());
        item.put("currentNode", order.getCurrentNode());
        item.put("applyReason", order.getApplyReason());
        item.put("approvalComment", order.getApprovalComment());
        item.put("appliedAt", order.getAppliedAt());
        item.put("decidedAt", order.getDecidedAt());
        item.put("records", approvalRecordRepository.findByApprovalOrderIdOrderByCreatedAtAsc(order.getId()));
        return item;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
