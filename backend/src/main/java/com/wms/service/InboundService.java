package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.InboundOrderItemRequest;
import com.wms.dto.InboundOrderRequest;
import com.wms.dto.InboundPickupActionRequest;
import com.wms.entity.Customer;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.Supplier;
import com.wms.entity.Warehouse;
import com.wms.repository.CustomerRepository;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.SupplierRepository;
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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboundService {

    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final StockService stockService;
    private final ExceptionService exceptionService;

    public List<Map<String, Object>> list() {
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        Map<Long, Supplier> supplierMap = supplierRepository.findAll().stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
        Map<Long, Customer> customerMap = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));

        return inboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> toOrderView(order, warehouseMap, supplierMap, customerMap))
                .toList();
    }

    @Transactional
    public Map<String, Object> create(InboundOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Inbound order requires at least one item");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found"));
        Customer customer = request.getCustomerId() == null ? null : customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found"));

        InboundOrder order = new InboundOrder();
        order.setOrderNo(optionalCode(request.getOrderNo(), "IN"));
        order.setWarehouseId(request.getWarehouseId());
        order.setSupplierId(request.getSupplierId());
        order.setOwnerId(request.getOwnerId());
        order.setCustomerId(request.getCustomerId());
        order.setOrderType(defaultText(request.getOrderType(), "PURCHASE"));
        order.setStatus("CREATED");
        order.setSourceNo(request.getSourceNo());
        order.setExpectedArrivalTime(request.getExpectedArrivalTime());
        order.setOperatorName(defaultText(request.getOperatorName(), "System Admin"));
        order.setReceiverName(defaultText(request.getReceiverName(), customer == null ? null : customer.getCustomerName()));
        order.setReceiverPhone(defaultText(request.getReceiverPhone(), customer == null ? null : customer.getContactPhone()));
        order.setScanCode(optionalCode(request.getScanCode(), "SCAN"));
        order.setRemark(request.getRemark());

        boolean pickupRequired = requiresCustomerPickup(warehouse, order);
        order.setPickupStatus(pickupRequired ? "WAITING_PUTAWAY" : "NOT_REQUIRED");
        order.setPickupCode(pickupRequired ? optionalCode(request.getPickupCode(), "PICK") : null);

        BigDecimal totalExpected = request.getItems().stream()
                .map(InboundOrderItemRequest::getExpectedQty)
                .map(this::defaultQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActual = request.getItems().stream()
                .map(item -> item.getActualQty() == null ? item.getExpectedQty() : item.getActualQty())
                .map(this::defaultQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalExpectedQty(totalExpected);
        order.setTotalActualQty(totalActual);

        InboundOrder savedOrder = inboundOrderRepository.save(order);
        saveItems(savedOrder, request.getItems(), warehouse);
        return detail(savedOrder.getId());
    }

    public Map<String, Object> detail(Long id) {
        InboundOrder order = inboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Inbound order not found"));
        return toOrderView(
                order,
                warehouseRepository.findAll().stream().collect(Collectors.toMap(Warehouse::getId, Function.identity())),
                supplierRepository.findAll().stream().collect(Collectors.toMap(Supplier::getId, Function.identity())),
                customerRepository.findAll().stream().collect(Collectors.toMap(Customer::getId, Function.identity()))
        );
    }

    @Transactional
    public Map<String, Object> receive(Long id) {
        InboundOrder order = inboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Inbound order not found"));

        if ("PUTAWAY_COMPLETED".equals(order.getStatus())) {
            return detail(id);
        }

        order.setStatus("RECEIVED");
        order.setActualArrivalTime(LocalDateTime.now());
        order.setReceivedAt(LocalDateTime.now());
        inboundOrderRepository.save(order);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> putaway(Long id) {
        InboundOrder order = inboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Inbound order not found"));
        Warehouse warehouse = warehouseRepository.findById(order.getWarehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found"));

        if ("PUTAWAY_COMPLETED".equals(order.getStatus())) {
            return detail(id);
        }

        if (order.getReceivedAt() == null) {
            order.setReceivedAt(LocalDateTime.now());
        }

        List<InboundOrderItem> items = inboundOrderItemRepository.findByOrderIdOrderByIdAsc(id);
        items.forEach(item -> {
            if (item.getLocationId() == null && Boolean.TRUE.equals(warehouse.getAutoAssignLocation())) {
                item.setLocationId(stockService.resolveInboundLocation(order.getWarehouseId(), defaultQty(item.getQualifiedQty())));
                inboundOrderItemRepository.save(item);
            }
            stockService.increaseStockFromInbound(order, item);
        });

        order.setStatus("PUTAWAY_COMPLETED");
        order.setPutawayCompletedAt(LocalDateTime.now());
        if (!"NOT_REQUIRED".equals(order.getPickupStatus())) {
            order.setPickupStatus("PENDING_PICKUP");
            if (warehouse.getDwellAlertMinutes() != null && warehouse.getDwellAlertMinutes() > 0) {
                order.setPickupDueAt(LocalDateTime.now().plusMinutes(warehouse.getDwellAlertMinutes()));
            }
        }
        inboundOrderRepository.save(order);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> handlePickupAction(Long id, InboundPickupActionRequest request) {
        InboundOrder order = inboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Inbound order not found"));
        Warehouse warehouse = warehouseRepository.findById(order.getWarehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found"));
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();

        switch (action) {
            case "PICKED_UP" -> markPickedUp(order, request);
            case "DELAY" -> extendPickup(order, warehouse, request);
            case "REFUSE" -> markRefused(order, request);
            default -> throw new BusinessException("Unsupported pickup action: " + action);
        }

        inboundOrderRepository.save(order);
        return detail(id);
    }

    private void markPickedUp(InboundOrder order, InboundPickupActionRequest request) {
        if ("PICKED_UP".equals(order.getPickupStatus())) {
            return;
        }
        List<InboundOrderItem> items = inboundOrderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        items.forEach(item -> stockService.decreaseStockForInboundPickup(
                order,
                item,
                defaultText(request.getOperatorName(), "PDA Device"),
                request.getRemark()
        ));
        order.setPickupStatus("PICKED_UP");
    }

    private void extendPickup(InboundOrder order, Warehouse warehouse, InboundPickupActionRequest request) {
        int delayMinutes = request.getExtendMinutes() == null || request.getExtendMinutes() <= 0
                ? Math.max(60, Optional.ofNullable(warehouse.getDwellAlertMinutes()).orElse(60))
                : request.getExtendMinutes();
        LocalDateTime base = order.getPickupDueAt() == null ? LocalDateTime.now() : order.getPickupDueAt();
        order.setPickupDueAt(base.plusMinutes(delayMinutes));
        order.setPickupStatus("DELAY_REQUESTED");
        exceptionService.createOrRefreshSystemTicket(
                order.getWarehouseId(),
                "INBOUND_ORDER",
                order.getId(),
                order.getOrderNo(),
                "Pickup delayed by customer",
                "Order " + order.getOrderNo() + " pickup deadline extended by " + delayMinutes + " minutes",
                "LOW",
                defaultText(request.getOperatorName(), "System")
        );
    }

    private void markRefused(InboundOrder order, InboundPickupActionRequest request) {
        order.setPickupStatus("REFUSED");
        exceptionService.createOrRefreshSystemTicket(
                order.getWarehouseId(),
                "INBOUND_ORDER",
                order.getId(),
                order.getOrderNo(),
                "Pickup refused by customer",
                "Order " + order.getOrderNo() + " has been marked as refused",
                "HIGH",
                defaultText(request.getOperatorName(), "System")
        );
    }

    private void saveItems(InboundOrder order, List<InboundOrderItemRequest> items, Warehouse warehouse) {
        items.forEach(itemRequest -> {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found, productId=" + itemRequest.getProductId()));

            InboundOrderItem item = new InboundOrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setSkuCode(product.getSkuCode());
            item.setProductName(product.getProductName());
            item.setBatchNo(itemRequest.getBatchNo());
            item.setProductionDate(itemRequest.getProductionDate());
            item.setExpiryDate(itemRequest.getExpiryDate());
            item.setExpectedQty(defaultQty(itemRequest.getExpectedQty()));
            item.setActualQty(defaultQty(itemRequest.getActualQty() == null ? itemRequest.getExpectedQty() : itemRequest.getActualQty()));
            item.setQualifiedQty(defaultQty(itemRequest.getQualifiedQty() == null
                    ? itemRequest.getActualQty() == null ? itemRequest.getExpectedQty() : itemRequest.getActualQty()
                    : itemRequest.getQualifiedQty()));

            Long locationId = itemRequest.getLocationId();
            if (locationId == null && Boolean.TRUE.equals(warehouse.getAutoAssignLocation())) {
                locationId = stockService.resolveInboundLocation(order.getWarehouseId(), defaultQty(item.getQualifiedQty()));
            }
            item.setLocationId(locationId);
            item.setRemark(itemRequest.getRemark());
            inboundOrderItemRepository.save(item);
        });
    }

    private Map<String, Object> toOrderView(
            InboundOrder order,
            Map<Long, Warehouse> warehouseMap,
            Map<Long, Supplier> supplierMap,
            Map<Long, Customer> customerMap
    ) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("orderNo", order.getOrderNo());
        item.put("warehouseId", order.getWarehouseId());
        item.put("warehouseName", Optional.ofNullable(warehouseMap.get(order.getWarehouseId())).map(Warehouse::getWarehouseName).orElse(null));
        item.put("supplierId", order.getSupplierId());
        item.put("supplierName", Optional.ofNullable(supplierMap.get(order.getSupplierId())).map(Supplier::getSupplierName).orElse(null));
        item.put("ownerId", order.getOwnerId());
        item.put("customerId", order.getCustomerId());
        item.put("customerName", Optional.ofNullable(customerMap.get(order.getCustomerId())).map(Customer::getCustomerName).orElse(null));
        item.put("receiverName", order.getReceiverName());
        item.put("receiverPhone", order.getReceiverPhone());
        item.put("pickupStatus", order.getPickupStatus());
        item.put("pickupCode", order.getPickupCode());
        item.put("pickupDueAt", order.getPickupDueAt());
        item.put("scanCode", order.getScanCode());
        item.put("orderType", order.getOrderType());
        item.put("status", order.getStatus());
        item.put("sourceNo", order.getSourceNo());
        item.put("expectedArrivalTime", order.getExpectedArrivalTime());
        item.put("actualArrivalTime", order.getActualArrivalTime());
        item.put("receivedAt", order.getReceivedAt());
        item.put("putawayCompletedAt", order.getPutawayCompletedAt());
        item.put("totalExpectedQty", order.getTotalExpectedQty());
        item.put("totalActualQty", order.getTotalActualQty());
        item.put("operatorName", order.getOperatorName());
        item.put("remark", order.getRemark());
        item.put("items", inboundOrderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        return item;
    }

    private boolean requiresCustomerPickup(Warehouse warehouse, InboundOrder order) {
        return order.getCustomerId() != null
                || (order.getReceiverPhone() != null && !order.getReceiverPhone().isBlank())
                || List.of("PARCEL_STATION", "TAKEOUT_LOCKER", "CAMPUS_PICKUP").contains(warehouse.getSceneType());
    }

    private String optionalCode(String value, String prefix) {
        if (value != null && !value.isBlank()) {
            return value;
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
