package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.BillingStatementGenerateRequest;
import com.wms.entity.BaseEntity;
import com.wms.entity.BillingContract;
import com.wms.entity.BillingRule;
import com.wms.entity.BillingStatement;
import com.wms.entity.BillingStatementItem;
import com.wms.entity.Customer;
import com.wms.entity.InboundOrder;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Stock;
import com.wms.entity.Warehouse;
import com.wms.repository.BillingContractRepository;
import com.wms.repository.BillingRuleRepository;
import com.wms.repository.BillingStatementItemRepository;
import com.wms.repository.BillingStatementRepository;
import com.wms.repository.CustomerRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.StockRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingContractRepository billingContractRepository;
    private final BillingRuleRepository billingRuleRepository;
    private final BillingStatementRepository billingStatementRepository;
    private final BillingStatementItemRepository billingStatementItemRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final StockRepository stockRepository;

    public Map<String, Object> overview() {
        Map<Long, Customer> customerMap = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        Map<String, Object> data = new HashMap<>();
        data.put("contracts", billingContractRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(contract -> toContractView(contract, customerMap, warehouseMap))
                .toList());
        data.put("rules", billingRuleRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        data.put("statements", billingStatementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(statement -> toStatementView(statement, customerMap))
                .toList());
        return data;
    }

    @Transactional
    public BillingContract saveContract(BillingContract contract) {
        requireNotNull(contract.getCustomerId(), "Customer is required");
        requireText(contract.getContractNo(), "Contract number is required");
        requireText(contract.getContractName(), "Contract name is required");
        requireNotNull(contract.getEffectiveDate(), "Effective date is required");
        if (contract.getSettlementCycle() == null || contract.getSettlementCycle().isBlank()) {
            contract.setSettlementCycle("MONTHLY");
        }
        if (contract.getStatus() == null || contract.getStatus().isBlank()) {
            contract.setStatus("ACTIVE");
        }
        if (contract.getId() != null) {
            billingContractRepository.findById(contract.getId())
                    .ifPresent(existing -> preserveAuditFields(contract, existing));
        }
        return billingContractRepository.save(contract);
    }

    @Transactional
    public BillingRule saveRule(BillingRule rule) {
        requireNotNull(rule.getContractId(), "Billing contract is required");
        requireText(rule.getChargeType(), "Charge type is required");
        requireText(rule.getRuleName(), "Rule name is required");
        requireText(rule.getUnitName(), "Unit name is required");
        if (rule.getStatus() == null || rule.getStatus().isBlank()) {
            rule.setStatus("ACTIVE");
        }
        if (rule.getUnitPrice() == null) {
            rule.setUnitPrice(BigDecimal.ZERO);
        }
        if (rule.getId() != null) {
            billingRuleRepository.findById(rule.getId())
                    .ifPresent(existing -> preserveAuditFields(rule, existing));
        }
        return billingRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        billingRuleRepository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> generateStatement(BillingStatementGenerateRequest request) {
        BillingContract contract = billingContractRepository.findById(request.getContractId())
                .orElseThrow(() -> new BusinessException("Billing contract not found"));
        String statementMonth = request.getStatementMonth();
        if (statementMonth == null || statementMonth.isBlank()) {
            throw new BusinessException("Statement month is required");
        }
        YearMonth yearMonth = YearMonth.parse(statementMonth);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        BillingStatement statement = new BillingStatement();
        statement.setStatementNo("ST" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        statement.setCustomerId(contract.getCustomerId());
        statement.setStatementMonth(statementMonth);
        statement.setTotalAmount(BigDecimal.ZERO);
        statement.setPaidAmount(BigDecimal.ZERO);
        statement.setStatementStatus("DRAFT");
        BillingStatement savedStatement = billingStatementRepository.save(statement);

        List<BillingRule> rules = billingRuleRepository.findByContractIdOrderByIdAsc(contract.getId()).stream()
                .filter(rule -> "ACTIVE".equals(rule.getStatus()))
                .toList();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (BillingRule rule : rules) {
            BigDecimal quantity = calculateRuleQuantity(contract, rule.getChargeType(), monthStart, monthEnd);
            BigDecimal amount = quantity.multiply(rule.getUnitPrice());
            BillingStatementItem item = new BillingStatementItem();
            item.setStatementId(savedStatement.getId());
            item.setChargeType(rule.getChargeType());
            item.setChargeName(rule.getRuleName());
            item.setQuantity(quantity);
            item.setUnitPrice(rule.getUnitPrice());
            item.setAmount(amount);
            item.setBizNo(statementMonth);
            billingStatementItemRepository.save(item);
            totalAmount = totalAmount.add(amount);
        }

        savedStatement.setTotalAmount(totalAmount);
        billingStatementRepository.save(savedStatement);

        Map<Long, Customer> customerMap = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        return toStatementView(savedStatement, customerMap);
    }

    @Transactional
    public Map<String, Object> markPaid(Long id) {
        BillingStatement statement = billingStatementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Billing statement not found"));
        statement.setPaidAmount(statement.getTotalAmount());
        statement.setStatementStatus("PAID");
        billingStatementRepository.save(statement);

        Map<Long, Customer> customerMap = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        return toStatementView(statement, customerMap);
    }

    private BigDecimal calculateRuleQuantity(BillingContract contract, String chargeType, LocalDateTime monthStart, LocalDateTime monthEnd) {
        return switch (chargeType) {
            case "OUTBOUND_ORDER_COUNT" -> BigDecimal.valueOf(outboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(order -> matchContract(contract, order.getWarehouseId(), order.getOwnerId()))
                    .filter(order -> inRange(order.getCreatedAt(), monthStart, monthEnd))
                    .count());
            case "OUTBOUND_QTY" -> outboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(order -> matchContract(contract, order.getWarehouseId(), order.getOwnerId()))
                    .filter(order -> inRange(resolveOutboundTime(order), monthStart, monthEnd))
                    .map(order -> defaultQty(order.getTotalShippedQty()).compareTo(BigDecimal.ZERO) > 0 ? order.getTotalShippedQty() : order.getTotalPlannedQty())
                    .map(this::defaultQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "INBOUND_ORDER_COUNT" -> BigDecimal.valueOf(inboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(order -> matchContract(contract, order.getWarehouseId(), order.getOwnerId()))
                    .filter(order -> inRange(order.getCreatedAt(), monthStart, monthEnd))
                    .count());
            case "INBOUND_QTY" -> inboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(order -> matchContract(contract, order.getWarehouseId(), order.getOwnerId()))
                    .filter(order -> inRange(resolveInboundTime(order), monthStart, monthEnd))
                    .map(InboundOrder::getTotalActualQty)
                    .map(this::defaultQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "STORAGE_SNAPSHOT_QTY" -> stockRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .filter(stock -> matchContract(contract, stock.getWarehouseId(), stock.getOwnerId()))
                    .map(Stock::getQuantity)
                    .map(this::defaultQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            default -> BigDecimal.ZERO;
        };
    }

    private boolean matchContract(BillingContract contract, Long warehouseId, Long ownerId) {
        boolean warehouseMatch = contract.getWarehouseId() == null || contract.getWarehouseId().equals(warehouseId);
        boolean ownerMatch = contract.getOwnerId() == null || contract.getOwnerId().equals(ownerId);
        return warehouseMatch && ownerMatch;
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null) {
            return false;
        }
        return !value.isBefore(start) && !value.isAfter(end);
    }

    private LocalDateTime resolveOutboundTime(OutboundOrder order) {
        return order.getShippedAt() != null ? order.getShippedAt() : order.getCreatedAt();
    }

    private LocalDateTime resolveInboundTime(InboundOrder order) {
        return order.getReceivedAt() != null ? order.getReceivedAt() : order.getCreatedAt();
    }

    private Map<String, Object> toContractView(BillingContract contract, Map<Long, Customer> customerMap, Map<Long, Warehouse> warehouseMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", contract.getId());
        item.put("customerId", contract.getCustomerId());
        item.put("customerName", customerMap.get(contract.getCustomerId()) == null ? null : customerMap.get(contract.getCustomerId()).getCustomerName());
        item.put("ownerId", contract.getOwnerId());
        item.put("warehouseId", contract.getWarehouseId());
        item.put("warehouseName", contract.getWarehouseId() == null || warehouseMap.get(contract.getWarehouseId()) == null ? null : warehouseMap.get(contract.getWarehouseId()).getWarehouseName());
        item.put("contractNo", contract.getContractNo());
        item.put("contractName", contract.getContractName());
        item.put("effectiveDate", contract.getEffectiveDate());
        item.put("expireDate", contract.getExpireDate());
        item.put("settlementCycle", contract.getSettlementCycle());
        item.put("status", contract.getStatus());
        item.put("rules", billingRuleRepository.findByContractIdOrderByIdAsc(contract.getId()));
        return item;
    }

    private Map<String, Object> toStatementView(BillingStatement statement, Map<Long, Customer> customerMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", statement.getId());
        item.put("statementNo", statement.getStatementNo());
        item.put("customerId", statement.getCustomerId());
        item.put("customerName", customerMap.get(statement.getCustomerId()) == null ? null : customerMap.get(statement.getCustomerId()).getCustomerName());
        item.put("statementMonth", statement.getStatementMonth());
        item.put("totalAmount", statement.getTotalAmount());
        item.put("paidAmount", statement.getPaidAmount());
        item.put("statementStatus", statement.getStatementStatus());
        item.put("items", billingStatementItemRepository.findByStatementIdOrderByIdAsc(statement.getId()));
        return item;
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
    }

    private void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
    }

    private void preserveAuditFields(BaseEntity target, BaseEntity existing) {
        target.setCreatedAt(existing.getCreatedAt());
        target.setUpdatedAt(existing.getUpdatedAt());
    }
}
