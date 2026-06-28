package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.entity.AlertEvent;
import com.wms.entity.AlertRule;
import com.wms.entity.InboundOrder;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Product;
import com.wms.entity.Stock;
import com.wms.entity.Warehouse;
import com.wms.repository.AlertEventRepository;
import com.wms.repository.AlertRuleRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.StockRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final NotificationService notificationService;
    private final ExceptionService exceptionService;

    public List<Map<String, Object>> listAlerts() {
        return alertEventRepository.findAllByOrderByLastTriggeredAtDesc().stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public Map<String, Object> acknowledge(Long id, String operatorName) {
        AlertEvent event = alertEventRepository.findById(id)
                .orElseThrow(() -> new BusinessException("预警记录不存在"));
        event.setStatus("ACKNOWLEDGED");
        event.setAcknowledgedBy(operatorName == null || operatorName.isBlank() ? "系统管理员" : operatorName);
        event.setAcknowledgedAt(LocalDateTime.now());
        return toView(alertEventRepository.save(event));
    }

    @Transactional
    public Map<String, Object> scanAlerts() {
        Map<String, AlertRule> rules = alertRuleRepository.findByEnabledTrueOrderByIdAsc().stream()
                .collect(Collectors.toMap(AlertRule::getRuleCode, Function.identity()));

        scanInboundReceiveTimeout(rules.get("INBOUND_RECEIVE_TIMEOUT"));
        scanInboundPutawayTimeout(rules.get("INBOUND_PUTAWAY_TIMEOUT"));
        scanOutboundShipTimeout(rules.get("OUTBOUND_SHIP_TIMEOUT"));
        scanStagnantStock(rules.get("STOCK_STAGNANT"));
        scanWarehousePickupDwell();

        Map<String, Object> result = new HashMap<>();
        result.put("enabledRuleCount", rules.size());
        result.put("openCount", alertEventRepository.countByStatus("OPEN"));
        result.put("acknowledgedCount", alertEventRepository.countByStatus("ACKNOWLEDGED"));
        result.put("resolvedCount", alertEventRepository.countByStatus("RESOLVED"));
        result.put("scannedAt", LocalDateTime.now());
        return result;
    }

    @Scheduled(fixedDelayString = "${app.alert.scan-fixed-delay-ms:300000}")
    public void scheduledScanAlerts() {
        try {
            scanAlerts();
        } catch (Exception ex) {
            log.error("alert scan failed", ex);
        }
    }

    @Transactional
    public Map<String, Object> deleteHistory(String period) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(resolveDeleteDays(period));
        long deletedCount = alertEventRepository.deleteByLastTriggeredAtBefore(cutoffTime);
        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("cutoffTime", cutoffTime);
        result.put("deletedCount", deletedCount);
        return result;
    }

    private void scanInboundReceiveTimeout(AlertRule rule) {
        if (rule == null) {
            return;
        }
        inboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> order.getExpectedArrivalTime() != null)
                .forEach(order -> {
                    String alertCode = rule.getRuleCode() + ":INBOUND_ORDER:" + order.getId();
                    boolean timeout = "CREATED".equals(order.getStatus())
                            && LocalDateTime.now().isAfter(order.getExpectedArrivalTime().plusMinutes(rule.getThresholdValue()));
                    if (timeout) {
                        openOrRefreshAlert(
                                rule,
                                "INBOUND_ORDER",
                                order.getId(),
                                "入库单 " + order.getOrderNo() + " 已超过 " + rule.getThresholdValue() + " 分钟仍未收货"
                        );
                    } else {
                        resolveAlertIfPresent(alertCode);
                    }
                });
    }

    private void scanInboundPutawayTimeout(AlertRule rule) {
        if (rule == null) {
            return;
        }
        inboundOrderRepository.findAllByOrderByCreatedAtDesc().forEach(order -> {
            String alertCode = rule.getRuleCode() + ":INBOUND_ORDER:" + order.getId();
            boolean timeout = order.getReceivedAt() != null
                    && order.getPutawayCompletedAt() == null
                    && LocalDateTime.now().isAfter(order.getReceivedAt().plusMinutes(rule.getThresholdValue()));
            if (timeout) {
                openOrRefreshAlert(
                        rule,
                        "INBOUND_ORDER",
                        order.getId(),
                        "入库单 " + order.getOrderNo() + " 收货后超过 " + rule.getThresholdValue() + " 分钟仍未上架"
                );
            } else {
                resolveAlertIfPresent(alertCode);
            }
        });
    }

    private void scanOutboundShipTimeout(AlertRule rule) {
        if (rule == null) {
            return;
        }
        outboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> order.getPlannedShipTime() != null)
                .forEach(order -> {
                    String alertCode = rule.getRuleCode() + ":OUTBOUND_ORDER:" + order.getId();
                    boolean timeout = order.getShippedAt() == null
                            && LocalDateTime.now().isAfter(order.getPlannedShipTime().plusMinutes(rule.getThresholdValue()));
                    if (timeout) {
                        openOrRefreshAlert(
                                rule,
                                "OUTBOUND_ORDER",
                                order.getId(),
                                "出库单 " + order.getOrderNo() + " 已超过计划发运时限 " + rule.getThresholdValue() + " 分钟"
                        );
                    } else {
                        resolveAlertIfPresent(alertCode);
                    }
                });
    }

    private void scanStagnantStock(AlertRule rule) {
        if (rule == null) {
            return;
        }
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        stockRepository.findAllByOrderByUpdatedAtDesc().forEach(stock -> {
            String alertCode = rule.getRuleCode() + ":INVENTORY_STOCK:" + stock.getId();
            boolean timeout = defaultQty(stock.getAvailableQty()).compareTo(BigDecimal.ZERO) > 0
                    && stock.getLastMovementAt() != null
                    && LocalDateTime.now().isAfter(stock.getLastMovementAt().plusDays(rule.getThresholdValue()));
            if (timeout) {
                openOrRefreshAlert(
                        rule,
                        "INVENTORY_STOCK",
                        stock.getId(),
                        "库存 " + Optional.ofNullable(productMap.get(stock.getProductId())).map(Product::getProductName).orElse("未知商品")
                                + " 已超过 " + rule.getThresholdValue() + " 天无库存变动"
                );
            } else {
                resolveAlertIfPresent(alertCode);
            }
        });
    }

    private void scanWarehousePickupDwell() {
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        inboundOrderRepository.findByPickupStatusInOrderByCreatedAtDesc(List.of("PENDING_PICKUP", "NOTICE_SENT", "DELAY_REQUESTED"))
                .forEach(order -> {
                    Warehouse warehouse = warehouseMap.get(order.getWarehouseId());
                    if (warehouse == null || order.getPickupDueAt() == null) {
                        return;
                    }

                    String alertCode = "WAREHOUSE_DWELL_TIMEOUT:INBOUND_ORDER:" + order.getId();
                    boolean timeout = LocalDateTime.now().isAfter(order.getPickupDueAt());
                    if (!timeout) {
                        resolveAlertIfPresent(alertCode);
                        return;
                    }

                    String message = buildPickupDwellMessage(warehouse, order);
                    openOrRefreshAlert("WAREHOUSE_DWELL_TIMEOUT", "客户取件滞留预警", "HIGH", "INBOUND_ORDER", order.getId(), message);
                    exceptionService.createOrRefreshSystemTicket(
                            order.getWarehouseId(),
                            "INBOUND_ORDER",
                            order.getId(),
                            order.getOrderNo(),
                            "客户取件滞留超时",
                            message,
                            "HIGH",
                            "预警服务"
                    );

                    if (shouldQueueSms(order, warehouse)) {
                        notificationService.queuePickupDwellSms(warehouse, order);
                        order.setLastSmsNoticeAt(LocalDateTime.now());
                        order.setPickupStatus("NOTICE_SENT");
                        inboundOrderRepository.save(order);
                    }
                });
    }

    private boolean shouldQueueSms(InboundOrder order, Warehouse warehouse) {
        if (!Boolean.TRUE.equals(warehouse.getSmsNotifyEnabled())) {
            return false;
        }
        if (order.getReceiverPhone() == null || order.getReceiverPhone().isBlank()) {
            return false;
        }
        if (order.getLastSmsNoticeAt() == null) {
            return true;
        }
        int interval = warehouse.getSmsReminderIntervalMinutes() == null || warehouse.getSmsReminderIntervalMinutes() <= 0
                ? 120
                : warehouse.getSmsReminderIntervalMinutes();
        return LocalDateTime.now().isAfter(order.getLastSmsNoticeAt().plusMinutes(interval));
    }

    private String buildPickupDwellMessage(Warehouse warehouse, InboundOrder order) {
        return "仓库 " + defaultText(warehouse.getWarehouseName(), "未命名仓库")
                + " 的入库单 " + order.getOrderNo()
                + " 已超过滞留时长，收件人："
                + defaultText(order.getReceiverName(), "未知")
                + "，联系电话："
                + defaultText(order.getReceiverPhone(), "-")
                + "，作业编码："
                + defaultText(order.getPickupCode(), defaultText(order.getScanCode(), "-"));
    }

    private void openOrRefreshAlert(AlertRule rule, String bizType, Long bizId, String message) {
        openOrRefreshAlert(rule.getRuleCode(), rule.getRuleName(), rule.getSeverity(), bizType, bizId, message);
    }

    private void openOrRefreshAlert(String ruleCode, String ruleName, String severity, String bizType, Long bizId, String message) {
        String alertCode = ruleCode + ":" + bizType + ":" + bizId;
        Optional<AlertEvent> existing = alertEventRepository.findFirstByAlertCodeAndStatusInOrderByIdDesc(
                alertCode,
                List.of("OPEN", "ACKNOWLEDGED")
        );

        AlertEvent event = existing.orElseGet(AlertEvent::new);
        if (event.getId() == null) {
            event.setAlertCode(alertCode);
            event.setFirstTriggeredAt(LocalDateTime.now());
        }
        event.setRuleCode(ruleCode);
        event.setRuleName(ruleName);
        event.setBizType(bizType);
        event.setBizId(bizId);
        event.setSeverity(severity);
        event.setAlertMessage(message);
        event.setStatus("OPEN");
        event.setLastTriggeredAt(LocalDateTime.now());
        alertEventRepository.save(event);
    }

    private void resolveAlertIfPresent(String alertCode) {
        alertEventRepository.findFirstByAlertCodeAndStatusInOrderByIdDesc(alertCode, List.of("OPEN", "ACKNOWLEDGED"))
                .ifPresent(event -> {
                    event.setStatus("RESOLVED");
                    event.setResolvedAt(LocalDateTime.now());
                    event.setLastTriggeredAt(LocalDateTime.now());
                    alertEventRepository.save(event);
                });
    }

    private Map<String, Object> toView(AlertEvent event) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", event.getId());
        item.put("alertCode", event.getAlertCode());
        item.put("ruleCode", event.getRuleCode());
        item.put("ruleName", event.getRuleName());
        item.put("bizType", event.getBizType());
        item.put("bizId", event.getBizId());
        item.put("severity", event.getSeverity());
        item.put("alertMessage", event.getAlertMessage());
        item.put("status", event.getStatus());
        item.put("firstTriggeredAt", event.getFirstTriggeredAt());
        item.put("lastTriggeredAt", event.getLastTriggeredAt());
        item.put("acknowledgedBy", event.getAcknowledgedBy());
        item.put("acknowledgedAt", event.getAcknowledgedAt());
        return item;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int resolveDeleteDays(String period) {
        String normalizedPeriod = period == null ? "" : period.trim().toUpperCase();
        return switch (normalizedPeriod) {
            case "ONE_WEEK", "WEEK", "7D" -> 7;
            case "ONE_MONTH", "MONTH", "1M" -> 30;
            case "THREE_MONTHS", "THREE_MONTH", "3M" -> 90;
            case "ONE_YEAR", "YEAR", "1Y" -> 365;
            default -> throw new BusinessException("不支持的删除周期: " + period);
        };
    }
}
