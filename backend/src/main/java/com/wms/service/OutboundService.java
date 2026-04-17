package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.OutboundOrderItemRequest;
import com.wms.dto.OutboundOrderRequest;
import com.wms.entity.Customer;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.Warehouse;
import com.wms.repository.CustomerRepository;
import com.wms.repository.OutboundOrderItemRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboundService {

    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final StockService stockService;

    public List<Map<String, Object>> list() {
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, warehouse -> warehouse));
        Map<Long, Customer> customerMap = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, customer -> customer));

        return outboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> toOrderView(order, warehouseMap, customerMap))
                .toList();
    }

    @Transactional
    public Map<String, Object> create(OutboundOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("出库单至少需要一条明细");
        }

        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(optionalOrderNo(request.getOrderNo(), "OUT"));
        order.setWarehouseId(request.getWarehouseId());
        order.setCustomerId(request.getCustomerId());
        order.setOwnerId(request.getOwnerId());
        order.setCarrierId(request.getCarrierId());
        order.setOrderType(defaultText(request.getOrderType(), "SALES"));
        order.setStatus("CREATED");
        order.setSourceNo(request.getSourceNo());
        order.setPriorityLevel(defaultText(request.getPriorityLevel(), "NORMAL"));
        order.setPlannedShipTime(request.getPlannedShipTime());
        order.setOperatorName(defaultText(request.getOperatorName(), "系统管理员"));
        order.setLogisticsNo(request.getLogisticsNo());
        order.setRemark(request.getRemark());

        BigDecimal totalPlanned = request.getItems().stream()
                .map(OutboundOrderItemRequest::getPlannedQty)
                .map(this::defaultQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalShipped = request.getItems().stream()
                .map(item -> item.getShippedQty() == null ? BigDecimal.ZERO : item.getShippedQty())
                .map(this::defaultQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPlannedQty(totalPlanned);
        order.setTotalShippedQty(totalShipped);

        OutboundOrder savedOrder = outboundOrderRepository.save(order);
        saveItems(savedOrder, request.getItems());

        return detail(savedOrder.getId());
    }

    public Map<String, Object> detail(Long id) {
        OutboundOrder order = outboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("出库单不存在"));
        return toOrderView(order, warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, warehouse -> warehouse)),
                customerRepository.findAll().stream().collect(Collectors.toMap(Customer::getId, customer -> customer)));
    }

    @Transactional
    public Map<String, Object> picking(Long id) {
        OutboundOrder order = outboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("出库单不存在"));
        if ("SHIPPED".equals(order.getStatus())) {
            return detail(id);
        }
        order.setStatus("PICKING_COMPLETED");
        order.setPickingCompletedAt(LocalDateTime.now());
        outboundOrderRepository.save(order);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> ship(Long id) {
        OutboundOrder order = outboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("出库单不存在"));
        if ("SHIPPED".equals(order.getStatus())) {
            return detail(id);
        }

        List<OutboundOrderItem> items = outboundOrderItemRepository.findByOrderIdOrderByIdAsc(id);
        items.forEach(item -> stockService.decreaseStockForOutbound(order, item));
        items.forEach(item -> {
            if (defaultQty(item.getShippedQty()).compareTo(BigDecimal.ZERO) <= 0) {
                item.setShippedQty(defaultQty(item.getPlannedQty()));
                outboundOrderItemRepository.save(item);
            }
        });

        BigDecimal totalShipped = items.stream()
                .map(OutboundOrderItem::getShippedQty)
                .map(this::defaultQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setStatus("SHIPPED");
        order.setShippedAt(LocalDateTime.now());
        order.setTotalShippedQty(totalShipped);
        if (order.getPickingCompletedAt() == null) {
            order.setPickingCompletedAt(LocalDateTime.now());
        }
        outboundOrderRepository.save(order);
        return detail(id);
    }

    private void saveItems(OutboundOrder order, List<OutboundOrderItemRequest> items) {
        items.forEach(itemRequest -> {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在，productId=" + itemRequest.getProductId()));

            OutboundOrderItem item = new OutboundOrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setSkuCode(product.getSkuCode());
            item.setProductName(product.getProductName());
            item.setBatchNo(itemRequest.getBatchNo());
            item.setPlannedQty(defaultQty(itemRequest.getPlannedQty()));
            item.setShippedQty(defaultQty(itemRequest.getShippedQty()));
            item.setLocationId(itemRequest.getLocationId());
            item.setRemark(itemRequest.getRemark());
            outboundOrderItemRepository.save(item);
        });
    }

    private Map<String, Object> toOrderView(OutboundOrder order, Map<Long, Warehouse> warehouseMap, Map<Long, Customer> customerMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("orderNo", order.getOrderNo());
        item.put("warehouseId", order.getWarehouseId());
        item.put("warehouseName", Optional.ofNullable(warehouseMap.get(order.getWarehouseId())).map(Warehouse::getWarehouseName).orElse(null));
        item.put("customerId", order.getCustomerId());
        item.put("customerName", Optional.ofNullable(customerMap.get(order.getCustomerId())).map(Customer::getCustomerName).orElse(null));
        item.put("ownerId", order.getOwnerId());
        item.put("carrierId", order.getCarrierId());
        item.put("orderType", order.getOrderType());
        item.put("status", order.getStatus());
        item.put("priorityLevel", order.getPriorityLevel());
        item.put("sourceNo", order.getSourceNo());
        item.put("plannedShipTime", order.getPlannedShipTime());
        item.put("pickingCompletedAt", order.getPickingCompletedAt());
        item.put("shippedAt", order.getShippedAt());
        item.put("totalPlannedQty", order.getTotalPlannedQty());
        item.put("totalShippedQty", order.getTotalShippedQty());
        item.put("operatorName", order.getOperatorName());
        item.put("logisticsNo", order.getLogisticsNo());
        item.put("remark", order.getRemark());
        item.put("items", outboundOrderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        return item;
    }

    private String optionalOrderNo(String orderNo, String prefix) {
        if (orderNo != null && !orderNo.isBlank()) {
            return orderNo;
        }
        return prefix + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
