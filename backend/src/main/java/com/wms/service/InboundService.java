package com.wms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.common.BusinessException;
import com.wms.common.BusinessCodeGenerator;
import com.wms.dto.InboundOrderItemRequest;
import com.wms.dto.InboundOrderRequest;
import com.wms.dto.InboundPickupActionRequest;
import com.wms.dto.InboundPutawayScanRequest;
import com.wms.dto.CodeRenderRequest;
import com.wms.entity.CargoCodeRecord;
import com.wms.entity.Customer;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Location;
import com.wms.entity.Product;
import com.wms.entity.Supplier;
import com.wms.entity.Warehouse;
import com.wms.repository.CargoCodeRecordRepository;
import com.wms.repository.CustomerRepository;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.SupplierRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final LocationRepository locationRepository;
    private final ScanService scanService;
    private final StockService stockService;
    private final ExceptionService exceptionService;
    private final CodeRenderService codeRenderService;
    private final CargoCodeRecordRepository cargoCodeRecordRepository;
    private final CargoCodeRecordService cargoCodeRecordService;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> list() {
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        Map<Long, Supplier> supplierMap = supplierRepository.findAll().stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
        Map<Long, Customer> customerMap = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));

        return inboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> toOrderView(order, warehouseMap, supplierMap, customerMap))
                .filter(order -> !Boolean.TRUE.equals(order.get("fullyTransferredOutbound")))
                .toList();
    }

    @Transactional
    public Map<String, Object> create(InboundOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Inbound order requires at least one item");
        }

        Supplier supplier = request.getSupplierId() == null ? null : supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new BusinessException("Supplier not found"));
        Long warehouseId = resolveWarehouseId(request.getWarehouseId(), supplier);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException("Warehouse not found"));
        Customer customer = request.getCustomerId() == null ? null : customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found"));

        String unifiedCode = BusinessCodeGenerator.unifiedOrderCode(request.getOrderNo());
        InboundOrder order = new InboundOrder();
        order.setOrderNo(unifiedCode);
        order.setWarehouseId(warehouseId);
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
        order.setScanCode(unifiedCode);
        order.setRemark(request.getRemark());

        boolean pickupRequired = requiresCustomerPickup(warehouse, order);
        order.setPickupStatus(pickupRequired ? "WAITING_PUTAWAY" : "NOT_REQUIRED");
        order.setPickupCode(pickupRequired ? unifiedCode : null);

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
        saveItems(savedOrder, request.getItems(), warehouse, supplier);
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

        List<InboundOrderItem> items = activeInboundItems(id);
        List<InboundOrderItem> unconfirmedItems = items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getPutawayScanConfirmed()))
                .toList();
        if (!unconfirmedItems.isEmpty()) {
            String codes = unconfirmedItems.stream()
                    .map(InboundOrderItem::getCargoCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("请先打印并扫描货物标签确认入库后再上架" + (codes.isBlank() ? "" : ": " + codes));
        }

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
    public Map<String, Object> scanPutaway(InboundPutawayScanRequest request) {
        if (request == null || request.getRawContent() == null || request.getRawContent().isBlank()) {
            throw new BusinessException("请先扫描货物标签");
        }

        com.wms.dto.ScanRecordRequest scanRecordRequest = new com.wms.dto.ScanRecordRequest();
        scanRecordRequest.setRawContent(request.getRawContent());
        scanRecordRequest.setScanFormat(request.getScanFormat());
        scanRecordRequest.setSourceDevice(defaultText(request.getSourceDevice(), "WEB_PDA"));
        scanRecordRequest.setScannerInterface(defaultText(request.getScannerInterface(), "WEB_MANUAL"));
        scanRecordRequest.setScannerDeviceId(request.getScannerDeviceId());
        scanRecordRequest.setOperatorName(defaultText(request.getOperatorName(), "系统管理员"));
        scanRecordRequest.setRemark(defaultText(request.getRemark(), "入库贴码扫码确认"));

        Map<String, Object> scanResult = scanService.saveRecord(scanRecordRequest);
        Map<String, Object> lookupData = castMap(scanResult.get("lookupData"));
        if (lookupData == null || !"INBOUND_ORDER_ITEM".equals(lookupData.get("entityType"))) {
            throw new BusinessException("请扫描货物标签上的二维码或条形码，当前扫码对象不是入库货物");
        }

        Long itemId = valueAsLong(lookupData.get("id"));
        InboundOrderItem item = inboundOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("入库货物明细不存在"));
        InboundOrder order = inboundOrderRepository.findById(item.getOrderId())
                .orElseThrow(() -> new BusinessException("入库单不存在"));
        if (item.getOutboundOrderId() != null) {
            throw new BusinessException("该货物已转入出库单 " + item.getOutboundOrderNo() + "，不能重复入库确认");
        }
        if ("PUTAWAY_COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("该货物所属入库单已完成上架，不能重复确认");
        }
        Warehouse warehouse = warehouseRepository.findById(order.getWarehouseId())
                .orElseThrow(() -> new BusinessException("仓库不存在"));
        if (item.getLocationId() == null) {
            if (Boolean.TRUE.equals(warehouse.getAutoAssignLocation())) {
                item.setLocationId(stockService.resolveInboundLocation(order.getWarehouseId(), defaultQty(item.getQualifiedQty())));
            } else {
                throw new BusinessException("该货物尚未分配库位，请先分配库位后再打印并扫码确认");
            }
        }
        Map<String, Object> recordData = castMap(scanResult.get("record"));
        item.setPutawayScanConfirmed(true);
        item.setPutawayScanConfirmedAt(LocalDateTime.now());
        item.setPutawayScanOperator(defaultText(request.getOperatorName(), "系统管理员"));
        item.setPutawayScanRecordId(recordData == null ? null : valueAsLong(recordData.get("id")));
        inboundOrderItemRepository.save(item);

        Map<String, Object> data = detail(order.getId());
        data.put("confirmedItemId", item.getId());
        data.put("confirmedCargoCode", item.getCargoCode());
        data.put("scanRecord", recordData);
        data.put("message", "货物标签扫码确认成功，确认后可正式上架入库");
        return data;
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

    private void saveItems(InboundOrder order, List<InboundOrderItemRequest> items, Warehouse warehouse, Supplier supplier) {
        items.forEach(itemRequest -> {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found, productId=" + itemRequest.getProductId()));

            InboundOrderItem item = new InboundOrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setSkuCode(product.getSkuCode());
            item.setProductName(product.getProductName());
            item.setBatchNo(itemRequest.getBatchNo());
            String requestedCargoCode = itemRequest.getCargoCode() == null || itemRequest.getCargoCode().isBlank()
                    ? null
                    : itemRequest.getCargoCode().trim();
            item.setCargoCode(requestedCargoCode);
            item.setCargoCodeType(resolveCargoCodeType(itemRequest.getCargoCodeType(), warehouse));
            item.setExternalPlatform(defaultText(itemRequest.getExternalPlatform(), supplier == null ? null : supplier.getPlatformType()));
            item.setExternalCode(itemRequest.getExternalCode());
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
            Location location = resolveLocationForLabel(locationId, order.getWarehouseId());
            item.setRemark(itemRequest.getRemark());
            InboundOrderItem savedItem = inboundOrderItemRepository.save(item);
            if (requestedCargoCode == null) {
                savedItem.setCargoCode(BusinessCodeGenerator.cargoCode(savedItem.getId()));
            }
            savedItem.setCargoCodeContent(buildCargoCodeContent(order, savedItem, product, warehouse, supplier, location));
            savedItem = inboundOrderItemRepository.save(savedItem);
            saveCargoCodeRecord(order, savedItem, warehouse, location);
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
        item.put("code", order.getOrderNo());
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
        List<InboundOrderItem> allItems = inboundOrderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        List<InboundOrderItem> activeItems = allItems.stream()
                .filter(itemEntity -> itemEntity.getOutboundOrderId() == null)
                .toList();
        item.put("items", activeItems.stream()
                .map(this::toItemView)
                .toList());
        item.put("transferredItemCount", allItems.size() - activeItems.size());
        item.put("fullyTransferredOutbound", !allItems.isEmpty() && activeItems.isEmpty());
        return item;
    }

    private Map<String, Object> toItemView(InboundOrderItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("orderId", item.getOrderId());
        data.put("productId", item.getProductId());
        data.put("skuCode", item.getSkuCode());
        data.put("productName", item.getProductName());
        data.put("batchNo", item.getBatchNo());
        data.put("cargoCode", item.getCargoCode());
        data.put("cargoCodeType", item.getCargoCodeType());
        data.put("cargoCodeContent", item.getCargoCodeContent());
        data.put("externalPlatform", item.getExternalPlatform());
        data.put("externalCode", item.getExternalCode());
        data.put("productionDate", item.getProductionDate());
        data.put("expiryDate", item.getExpiryDate());
        data.put("expectedQty", item.getExpectedQty());
        data.put("actualQty", item.getActualQty());
        data.put("qualifiedQty", item.getQualifiedQty());
        data.put("locationId", item.getLocationId());
        locationRepository.findById(Optional.ofNullable(item.getLocationId()).orElse(-1L)).ifPresent(location -> {
            data.put("locationCode", location.getLocationCode());
            data.put("locationName", location.getLocationName());
            data.put("zoneName", location.getZoneName());
            data.put("aisleNo", location.getAisleNo());
            data.put("shelfNo", location.getShelfNo());
            data.put("layerNo", location.getLayerNo());
            data.put("binNo", location.getBinNo());
            data.put("locationFullName", locationFullName(location));
        });
        data.put("putawayScanConfirmed", Boolean.TRUE.equals(item.getPutawayScanConfirmed()));
        data.put("putawayScanConfirmedAt", item.getPutawayScanConfirmedAt());
        data.put("putawayScanOperator", item.getPutawayScanOperator());
        data.put("putawayScanRecordId", item.getPutawayScanRecordId());
        data.put("outboundOrderId", item.getOutboundOrderId());
        data.put("outboundOrderNo", item.getOutboundOrderNo());
        data.put("outboundTransferredAt", item.getOutboundTransferredAt());
        data.put("remark", item.getRemark());
        List<Map<String, Object>> codeRecords = cargoCodeRecordRepository.findByInboundOrderItemIdOrderByIdAsc(item.getId()).stream()
                .map(cargoCodeRecordService::toView)
                .toList();
        data.put("cargoCodeRecords", codeRecords);
        data.put("cargoCodeSvg", codeRecords.isEmpty() ? null : codeRecords.get(codeRecords.size() - 1).get("svgContent"));
        return data;
    }

    private List<InboundOrderItem> activeInboundItems(Long orderId) {
        return inboundOrderItemRepository.findByOrderIdOrderByIdAsc(orderId).stream()
                .filter(item -> item.getOutboundOrderId() == null)
                .toList();
    }

    private void saveCargoCodeRecord(InboundOrder order, InboundOrderItem item, Warehouse warehouse, Location location) {
        String rawContent = codeRawContent(item);
        CodeRenderRequest renderRequest = new CodeRenderRequest();
        renderRequest.setRawContent(rawContent);
        renderRequest.setScanFormat(item.getCargoCodeType());
        renderRequest.setWidth("BAR_CODE".equals(item.getCargoCodeType()) ? 520 : 240);
        renderRequest.setHeight("BAR_CODE".equals(item.getCargoCodeType()) ? 140 : 240);
        Map<String, Object> rendered = codeRenderService.render(renderRequest);

        CargoCodeRecord record = new CargoCodeRecord();
        record.setInboundOrderId(order.getId());
        record.setInboundOrderItemId(item.getId());
        record.setWarehouseId(order.getWarehouseId());
        record.setProductId(item.getProductId());
        record.setOperationType("INBOUND_PUTAWAY");
        record.setOperationCode(order.getOrderNo());
        record.setLocationId(item.getLocationId());
        record.setLocationCode(location == null ? null : location.getLocationCode());
        record.setLocationName(location == null ? null : location.getLocationName());
        record.setZoneName(location == null ? null : location.getZoneName());
        record.setCargoCode(item.getCargoCode());
        record.setCargoCodeType(item.getCargoCodeType());
        record.setRawContent(rawContent);
        record.setSvgContent((String) rendered.get("svg"));
        record.setRenderFormat((String) rendered.get("scanFormat"));
        record.setRenderWidth(renderRequest.getWidth());
        record.setRenderHeight(renderRequest.getHeight());
        record.setExternalPlatform(item.getExternalPlatform());
        record.setExternalCode(item.getExternalCode());
        record.setStatus("ACTIVE");
        record.setRemark("自动生成入库货物码");
        cargoCodeRecordRepository.save(record);
    }

    private String codeRawContent(InboundOrderItem item) {
        return "BAR_CODE".equals(item.getCargoCodeType())
                ? item.getCargoCode()
                : defaultText(item.getCargoCodeContent(), item.getCargoCode());
    }

    private String resolveCargoCodeType(String requestedType, Warehouse warehouse) {
        String type = requestedType == null || requestedType.isBlank()
                ? warehouse.getScanMode()
                : requestedType;
        if ("HYBRID".equalsIgnoreCase(type)) {
            return "QR_CODE";
        }
        if ("BAR_CODE".equalsIgnoreCase(type) || "BARCODE".equalsIgnoreCase(type)) {
            return "BAR_CODE";
        }
        return "QR_CODE";
    }

    private String buildCargoCodeContent(
            InboundOrder order,
            InboundOrderItem item,
            Product product,
            Warehouse warehouse,
            Supplier supplier,
            Location location
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", item.getCargoCode());
        payload.put("cargoCode", item.getCargoCode());
        payload.put("orderNo", order.getOrderNo());
        payload.put("workCode", order.getOrderNo());
        payload.put("operationType", "INBOUND_PUTAWAY");
        payload.put("operationName", "入库上架");
        payload.put("warehouseId", order.getWarehouseId());
        payload.put("warehouseCode", warehouse.getWarehouseCode());
        payload.put("warehouseName", warehouse.getWarehouseName());
        payload.put("supplierId", order.getSupplierId());
        payload.put("supplierCode", supplier == null ? null : supplier.getSupplierCode());
        payload.put("supplierName", supplier == null ? null : supplier.getSupplierName());
        payload.put("platform", item.getExternalPlatform());
        payload.put("externalCode", item.getExternalCode());
        payload.put("productId", item.getProductId());
        payload.put("skuCode", item.getSkuCode());
        payload.put("productName", item.getProductName());
        payload.put("productBarcode", product.getBarcode());
        payload.put("batchNo", item.getBatchNo());
        payload.put("expectedQty", item.getExpectedQty());
        payload.put("actualQty", item.getActualQty());
        payload.put("qualifiedQty", item.getQualifiedQty());
        payload.put("locationId", item.getLocationId());
        payload.put("locationCode", location == null ? null : location.getLocationCode());
        payload.put("locationName", location == null ? null : location.getLocationName());
        payload.put("zoneName", location == null ? null : location.getZoneName());
        payload.put("aisleNo", location == null ? null : location.getAisleNo());
        payload.put("shelfNo", location == null ? null : location.getShelfNo());
        payload.put("layerNo", location == null ? null : location.getLayerNo());
        payload.put("binNo", location == null ? null : location.getBinNo());
        payload.put("locationFullName", locationFullName(location));
        payload.put("productionDate", item.getProductionDate());
        payload.put("expiryDate", item.getExpiryDate());
        payload.put("source", "WMS_INBOUND");
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new BusinessException("货物码内容生成失败: " + exception.getMessage());
        }
    }

    private Location resolveLocationForLabel(Long locationId, Long warehouseId) {
        if (locationId == null) {
            throw new BusinessException("入库货物必须先分配库位，才能生成可打印的二维码/条形码标签");
        }
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException("库位不存在，无法生成货物标签"));
        if (!Objects.equals(location.getWarehouseId(), warehouseId)) {
            throw new BusinessException("货物库位不属于当前仓库，无法生成货物标签");
        }
        return location;
    }

    private String locationFullName(Location location) {
        if (location == null) {
            return null;
        }
        return List.of(
                        location.getZoneName(),
                        location.getAisleNo(),
                        location.getShelfNo(),
                        location.getLayerNo(),
                        location.getBinNo(),
                        location.getLocationCode(),
                        location.getLocationName()
                ).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining(" / "));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            map.forEach((key, item) -> {
                if (key != null) {
                    result.put(key.toString(), item);
                }
            });
            return result;
        }
        return null;
    }

    private Long valueAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long resolveWarehouseId(Long requestWarehouseId, Supplier supplier) {
        if (supplier != null && supplier.getWarehouseId() != null) {
            return supplier.getWarehouseId();
        }
        if (requestWarehouseId == null) {
            throw new BusinessException("Warehouse not found");
        }
        return requestWarehouseId;
    }

    private boolean requiresCustomerPickup(Warehouse warehouse, InboundOrder order) {
        return order.getCustomerId() != null
                || (order.getReceiverPhone() != null && !order.getReceiverPhone().isBlank())
                || List.of("PARCEL_STATION", "TAKEOUT_LOCKER", "CAMPUS_PICKUP").contains(warehouse.getSceneType());
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
