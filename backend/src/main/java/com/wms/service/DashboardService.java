package com.wms.service;

import com.wms.dto.DashboardOverviewResponse;
import com.wms.entity.AlertEvent;
import com.wms.entity.InboundOrder;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Stock;
import com.wms.repository.AlertEventRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.StockRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final AlertEventRepository alertEventRepository;
    private final StockRepository stockRepository;

    public DashboardOverviewResponse overview() {
        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setWarehouseCount(warehouseRepository.count());
        response.setProductCount(productRepository.count());
        response.setPendingInboundCount(inboundOrderRepository.countByStatus("CREATED") + inboundOrderRepository.countByStatus("RECEIVED"));
        response.setPendingOutboundCount(outboundOrderRepository.countByStatus("CREATED") + outboundOrderRepository.countByStatus("PICKING_COMPLETED"));
        response.setUnresolvedAlertCount(alertEventRepository.countByStatus("OPEN"));
        response.setTotalStockQuantity(stockRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(Stock::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setRecentInbounds(inboundOrderRepository.findAllByOrderByCreatedAtDesc().stream().limit(5).map(this::toInboundSummary).toList());
        response.setRecentOutbounds(outboundOrderRepository.findAllByOrderByCreatedAtDesc().stream().limit(5).map(this::toOutboundSummary).toList());
        response.setHotAlerts(alertEventRepository.findAllByOrderByLastTriggeredAtDesc().stream().limit(5).map(this::toAlertSummary).toList());
        return response;
    }

    private Map<String, Object> toInboundSummary(InboundOrder order) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("orderNo", order.getOrderNo());
        item.put("status", order.getStatus());
        item.put("warehouseId", order.getWarehouseId());
        item.put("totalExpectedQty", order.getTotalExpectedQty());
        item.put("receivedAt", order.getReceivedAt());
        item.put("createdAt", order.getCreatedAt());
        return item;
    }

    private Map<String, Object> toOutboundSummary(OutboundOrder order) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("orderNo", order.getOrderNo());
        item.put("status", order.getStatus());
        item.put("warehouseId", order.getWarehouseId());
        item.put("totalPlannedQty", order.getTotalPlannedQty());
        item.put("plannedShipTime", order.getPlannedShipTime());
        item.put("createdAt", order.getCreatedAt());
        return item;
    }

    private Map<String, Object> toAlertSummary(AlertEvent event) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", event.getId());
        item.put("ruleName", event.getRuleName());
        item.put("severity", event.getSeverity());
        item.put("status", event.getStatus());
        item.put("alertMessage", event.getAlertMessage());
        item.put("lastTriggeredAt", event.getLastTriggeredAt());
        return item;
    }
}
