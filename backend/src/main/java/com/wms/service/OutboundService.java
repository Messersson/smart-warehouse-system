package com.wms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.common.BusinessException;
import com.wms.common.BusinessCodeGenerator;
import com.wms.dto.OutboundOrderItemRequest;
import com.wms.dto.OutboundOrderRequest;
import com.wms.dto.OutboundScanRequest;
import com.wms.entity.Customer;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.ScanRecord;
import com.wms.entity.Warehouse;
import com.wms.repository.CustomerRepository;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.OutboundOrderItemRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.ScanRecordRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboundService {

    private static final List<String> PRODUCT_CODE_KEYS = List.of("cargoCode", "barcode", "barCode", "skuCode", "sku", "code", "productCode");
    private static final Pattern PRODUCT_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)(cargoCode|barcode|barCode|skuCode|sku|code|productCode)\\s*[:=]\\s*([^\\s,;|]+)"
    );
    private static final Pattern GS1_GTIN_PATTERN = Pattern.compile("\\(01\\)(\\d{14})");

    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final ScanRecordRepository scanRecordRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final StockService stockService;
    private final ObjectMapper objectMapper;

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
        String unifiedCode = BusinessCodeGenerator.unifiedOrderCode(request.getOrderNo());
        order.setOrderNo(unifiedCode);
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
        order.setLogisticsNo(defaultText(request.getLogisticsNo(), unifiedCode));
        order.setRemark(request.getRemark());

        BigDecimal totalPlanned = request.getItems().stream()
                .map(OutboundOrderItemRequest::getPlannedQty)
                .map(this::defaultQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPlannedQty(totalPlanned);
        order.setTotalShippedQty(BigDecimal.ZERO);

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
        validateAllItemsScanned(items);
        items.forEach(item -> stockService.decreaseStockForOutbound(order, item));

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

    @Transactional
    public Map<String, Object> scanShip(Long id, OutboundScanRequest request) {
        OutboundOrder order = outboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("出库单不存在"));
        if ("SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("出库单已发运，不能继续扫码");
        }

        String rawContent = request == null ? "" : defaultText(request.getRawContent(), "");
        String scanCode = extractScanCode(rawContent);
        ScannedProduct scannedProduct = resolveScannedProduct(scanCode);
        Product product = scannedProduct.product();

        List<OutboundOrderItem> items = outboundOrderItemRepository.findByOrderIdOrderByIdAsc(id);
        OutboundOrderItem matchedItem = items.stream()
                .filter(item -> item.getProductId().equals(product.getId())
                        || (item.getSkuCode() != null && item.getSkuCode().equalsIgnoreCase(scanCode)))
                .filter(item -> defaultQty(item.getShippedQty()).compareTo(defaultQty(item.getPlannedQty())) < 0)
                .findFirst()
                .orElseThrow(() -> new BusinessException("扫码商品不属于该出库单，或该商品已扫码完成: " + scanCode));

        BigDecimal plannedQty = defaultQty(matchedItem.getPlannedQty());
        BigDecimal nextScannedQty = defaultQty(matchedItem.getShippedQty()).add(BigDecimal.ONE).min(plannedQty);
        matchedItem.setShippedQty(nextScannedQty);
        outboundOrderItemRepository.save(matchedItem);

        BigDecimal totalScanned = outboundOrderItemRepository.findByOrderIdOrderByIdAsc(id).stream()
                .map(OutboundOrderItem::getShippedQty)
                .map(this::defaultQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalShippedQty(totalScanned);
        if (!"PICKING_COMPLETED".equals(order.getStatus())) {
            order.setStatus("PICKING_COMPLETED");
            order.setPickingCompletedAt(LocalDateTime.now());
        }
        outboundOrderRepository.save(order);

        saveScanRecord(order, matchedItem, product, rawContent, scanCode, scannedProduct.codeType(), request);
        return detail(id);
    }

    private ScannedProduct resolveScannedProduct(String scanCode) {
        Optional<InboundOrderItem> inboundItem = inboundOrderItemRepository.findFirstByCargoCodeOrExternalCodeOrderByIdDesc(scanCode, scanCode)
                .or(() -> inboundOrderItemRepository.findFirstByCargoCodeContentOrderByIdDesc(scanCode));
        if (inboundItem.isPresent()) {
            InboundOrderItem item = inboundItem.get();
            Product product = productRepository.findById(item.getProductId())
                    .orElseGet(() -> productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(item.getSkuCode(), item.getSkuCode())
                            .orElseThrow(() -> new BusinessException("货物码已识别，但商品档案不存在: " + scanCode)));
            return new ScannedProduct(product, "CARGO_CODE");
        }

        Product product = productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(scanCode, scanCode)
                .orElseThrow(() -> new BusinessException("扫码商品不在商品档案中: " + scanCode));
        return new ScannedProduct(product, "PRODUCT");
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
            item.setShippedQty(BigDecimal.ZERO);
            item.setLocationId(itemRequest.getLocationId());
            item.setRemark(itemRequest.getRemark());
            outboundOrderItemRepository.save(item);
        });
    }

    private Map<String, Object> toOrderView(OutboundOrder order, Map<Long, Warehouse> warehouseMap, Map<Long, Customer> customerMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("code", order.getOrderNo());
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

    private void validateAllItemsScanned(List<OutboundOrderItem> items) {
        Optional<OutboundOrderItem> unscannedItem = items.stream()
                .filter(item -> defaultQty(item.getShippedQty()).compareTo(defaultQty(item.getPlannedQty())) < 0)
                .findFirst();
        if (unscannedItem.isPresent()) {
            OutboundOrderItem item = unscannedItem.get();
            throw new BusinessException("商品 " + item.getSkuCode() + " 未扫码完成，计划 "
                    + defaultQty(item.getPlannedQty()) + "，已扫 " + defaultQty(item.getShippedQty()));
        }
    }

    private String extractScanCode(String rawContent) {
        String raw = rawContent == null ? "" : rawContent.trim();
        if (raw.isBlank()) {
            throw new BusinessException("扫码内容不能为空");
        }
        return parseJsonCode(raw)
                .or(() -> parseUriCode(raw))
                .or(() -> parseKeyValueCode(raw))
                .or(() -> parseGs1Code(raw))
                .orElse(raw);
    }

    private Optional<String> parseJsonCode(String raw) {
        if (!(raw.startsWith("{") && raw.endsWith("}"))) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(raw, new TypeReference<>() {
            });
            return findProductCode(payload);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<String> parseUriCode(String raw) {
        if (!raw.contains("?") && !raw.contains("://")) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(raw);
            Map<String, String> query = splitQuery(uri.getRawQuery());
            return PRODUCT_CODE_KEYS.stream()
                    .map(key -> findValueIgnoreCase(query, key))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .or(() -> {
                        String path = uri.getPath();
                        if (path == null || path.isBlank()) {
                            return Optional.empty();
                        }
                        String[] parts = path.split("/");
                        for (int i = parts.length - 1; i >= 0; i--) {
                            String part = urlDecode(parts[i]);
                            if (!part.isBlank()) {
                                return Optional.of(part);
                            }
                        }
                        return Optional.empty();
                    });
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<String> parseKeyValueCode(String raw) {
        Matcher matcher = PRODUCT_KEY_VALUE_PATTERN.matcher(raw);
        return matcher.find() ? Optional.of(matcher.group(2).trim()) : Optional.empty();
    }

    private Optional<String> parseGs1Code(String raw) {
        Matcher matcher = GS1_GTIN_PATTERN.matcher(raw);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private Optional<String> findProductCode(Map<String, Object> payload) {
        return PRODUCT_CODE_KEYS.stream()
                .map(key -> findValueIgnoreCase(payload, key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .or(() -> payload.values().stream()
                        .filter(Map.class::isInstance)
                        .map(value -> (Map<?, ?>) value)
                        .map(this::stringKeyMap)
                        .map(this::findProductCode)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst());
    }

    private Optional<String> findValueIgnoreCase(Map<String, ?> payload, String targetKey) {
        return payload.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(targetKey))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.toString().isBlank())
                .map(value -> value.toString().trim())
                .findFirst();
    }

    private Map<String, Object> stringKeyMap(Map<?, ?> payload) {
        Map<String, Object> result = new HashMap<>();
        payload.forEach((key, value) -> {
            if (key != null) {
                result.put(key.toString(), value);
            }
        });
        return result;
    }

    private Map<String, String> splitQuery(String rawQuery) {
        Map<String, String> result = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }
        for (String part : rawQuery.split("&")) {
            int index = part.indexOf('=');
            if (index <= 0) {
                continue;
            }
            result.put(urlDecode(part.substring(0, index)), urlDecode(part.substring(index + 1)));
        }
        return result;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void saveScanRecord(OutboundOrder order, OutboundOrderItem item, Product product, String rawContent, String scanCode, String codeType, OutboundScanRequest request) {
        ScanRecord record = new ScanRecord();
        record.setRawContent(rawContent);
        record.setParsedCode(scanCode);
        record.setScanFormat(defaultText(request == null ? null : request.getScanFormat(), "AUTO"));
        record.setContentFormat("OUTBOUND_SCAN");
        record.setCodeType(codeType);
        record.setMatched(true);
        record.setEntityType("OUTBOUND_ORDER_ITEM");
        record.setEntityId(item.getId());
        record.setWarehouseId(order.getWarehouseId());
        record.setSourceDevice(defaultText(request == null ? null : request.getSourceDevice(), "WEB_OUTBOUND"));
        record.setScannerInterface(defaultText(request == null ? null : request.getScannerInterface(), "WEB_MANUAL"));
        record.setScannerDeviceId(request == null ? null : request.getScannerDeviceId());
        record.setOperatorName(defaultText(request == null ? null : request.getOperatorName(), order.getOperatorName()));
        record.setRemark("出库扫码确认: " + product.getSkuCode());
        scanRecordRepository.save(record);
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record ScannedProduct(Product product, String codeType) {
    }
}
