package com.wms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.common.BusinessException;
import com.wms.common.BusinessCodeGenerator;
import com.wms.dto.OutboundOrderItemRequest;
import com.wms.dto.OutboundOrderRequest;
import com.wms.dto.OutboundScanRequest;
import com.wms.entity.CargoCodeRecord;
import com.wms.entity.Customer;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.ScanRecord;
import com.wms.entity.Warehouse;
import com.wms.repository.CargoCodeRecordRepository;
import com.wms.repository.CustomerRepository;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboundService {

    private static final List<String> PRODUCT_CODE_KEYS = List.of(
            "cargoCode", "externalCode", "externalNo", "merchantCode",
            "barcode", "barCode", "bar_code", "skuCode", "sku_code", "sku",
            "code", "productCode", "product_code", "waybillCode", "waybillNo",
            "mailNo", "mailno", "expressNo", "expressCode", "logisticsNo",
            "trackingNo", "trackingNumber", "packageId", "packageNo", "packageCode",
            "parcelNo", "parcelCode", "mtOrderId", "meituanOrderId", "wmOrderId",
            "tbOrderId", "tmallOrderId", "pddOrderSn"
    );
    private static final Pattern PRODUCT_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)(cargoCode|externalCode|externalNo|merchantCode|barcode|barCode|bar_code|skuCode|sku_code|sku|code|productCode|product_code|waybillCode|waybillNo|mailNo|mailno|expressNo|expressCode|logisticsNo|trackingNo|trackingNumber|packageId|packageNo|packageCode|parcelNo|parcelCode|mtOrderId|meituanOrderId|wmOrderId|tbOrderId|tmallOrderId|pddOrderSn|货物码|外部码|商家码|快递单号|运单号|物流单号|包裹号|商品条码|条形码|商品编码)\\s*[:=：]\\s*([^\\s,;|，；]+)"
    );
    private static final Pattern EXPRESS_LIKE_PATTERN = Pattern.compile("\\b([A-Z]{1,6}\\d{6,24}|\\d{10,24})\\b");
    private static final Pattern GS1_GTIN_PATTERN = Pattern.compile("\\(01\\)(\\d{14})");

    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final CargoCodeRecordRepository cargoCodeRecordRepository;
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
        List<OutboundOrderItem> savedItems = saveItems(savedOrder, request.getItems());
        savedItems.forEach(item -> stockService.lockStockForOutbound(savedOrder, item));

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
        ScannedProduct scannedProduct = extractScanCandidates(rawContent).stream()
                .map(this::tryResolveScannedProduct)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new BusinessException("扫码商品不在商品档案或货物码记录中: " + rawContent));
        String scanCode = scannedProduct.scanCode();
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

    @Transactional
    public Map<String, Object> scanTransfer(OutboundScanRequest request) {
        String rawContent = request == null ? "" : defaultText(request.getRawContent(), "");
        ScannedInboundCargo scannedCargo = extractScanCandidates(rawContent).stream()
                .map(this::tryResolveScannedInboundCargo)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到可转出库的入库货物: " + rawContent));

        InboundOrderItem inboundItem = scannedCargo.inboundItem();
        if (inboundItem.getOutboundOrderId() != null) {
            Map<String, Object> data = detail(inboundItem.getOutboundOrderId());
            data.put("message", "该货物已转入出库单 " + inboundItem.getOutboundOrderNo());
            return data;
        }
        if (!Boolean.TRUE.equals(inboundItem.getPutawayScanConfirmed())) {
            throw new BusinessException("该货物尚未扫码确认入库，不能直接出库: " + scannedCargo.scanCode());
        }

        InboundOrder inboundOrder = inboundOrderRepository.findById(inboundItem.getOrderId())
                .orElseThrow(() -> new BusinessException("入库货物所属入库单不存在"));
        Product product = scannedCargo.product();
        BigDecimal transferQty = transferQuantity(inboundItem);
        String operatorName = defaultText(request == null ? null : request.getOperatorName(), "系统管理员");
        String outboundNo = BusinessCodeGenerator.unifiedOrderCode(null);

        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOrderNo(outboundNo);
        outboundOrder.setWarehouseId(inboundOrder.getWarehouseId());
        outboundOrder.setCustomerId(inboundOrder.getCustomerId());
        outboundOrder.setOwnerId(inboundOrder.getOwnerId());
        outboundOrder.setOrderType("SCAN_TRANSFER");
        outboundOrder.setStatus("SHIPPED");
        outboundOrder.setSourceNo(inboundOrder.getOrderNo());
        outboundOrder.setPriorityLevel("NORMAL");
        outboundOrder.setPlannedShipTime(LocalDateTime.now());
        outboundOrder.setPickingCompletedAt(LocalDateTime.now());
        outboundOrder.setShippedAt(LocalDateTime.now());
        outboundOrder.setTotalPlannedQty(transferQty);
        outboundOrder.setTotalShippedQty(transferQty);
        outboundOrder.setOperatorName(operatorName);
        outboundOrder.setLogisticsNo(defaultText(scannedCargo.scanCode(), outboundNo));
        outboundOrder.setRemark("扫码从入库单 " + inboundOrder.getOrderNo() + " 转出");
        OutboundOrder savedOrder = outboundOrderRepository.save(outboundOrder);

        OutboundOrderItem outboundItem = new OutboundOrderItem();
        outboundItem.setOrderId(savedOrder.getId());
        outboundItem.setProductId(product.getId());
        outboundItem.setSkuCode(product.getSkuCode());
        outboundItem.setProductName(product.getProductName());
        outboundItem.setBatchNo(inboundItem.getBatchNo());
        outboundItem.setPlannedQty(transferQty);
        outboundItem.setShippedQty(transferQty);
        outboundItem.setLocationId(inboundItem.getLocationId());
        outboundItem.setRemark("由入库货物码转出: " + scannedCargo.scanCode());
        OutboundOrderItem savedItem = outboundOrderItemRepository.save(outboundItem);

        if ("PUTAWAY_COMPLETED".equals(inboundOrder.getStatus())) {
            stockService.decreaseStockForOutbound(savedOrder, savedItem);
        }

        inboundItem.setPutawayScanConfirmed(true);
        if (inboundItem.getPutawayScanConfirmedAt() == null) {
            inboundItem.setPutawayScanConfirmedAt(LocalDateTime.now());
        }
        inboundItem.setPutawayScanOperator(operatorName);
        inboundItem.setOutboundOrderId(savedOrder.getId());
        inboundItem.setOutboundOrderNo(savedOrder.getOrderNo());
        inboundItem.setOutboundTransferredAt(LocalDateTime.now());
        inboundOrderItemRepository.save(inboundItem);

        boolean allTransferred = inboundOrderItemRepository.findByOrderIdOrderByIdAsc(inboundOrder.getId()).stream()
                .allMatch(item -> item.getOutboundOrderId() != null);
        if (allTransferred) {
            inboundOrder.setPickupStatus("PICKED_UP");
            inboundOrderRepository.save(inboundOrder);
        }

        saveScanRecord(savedOrder, savedItem, product, rawContent, scannedCargo.scanCode(), scannedCargo.codeType(), request);
        Map<String, Object> data = detail(savedOrder.getId());
        data.put("sourceInboundOrderId", inboundOrder.getId());
        data.put("sourceInboundOrderNo", inboundOrder.getOrderNo());
        data.put("sourceInboundItemId", inboundItem.getId());
        data.put("message", "货物已从入库单转入出库单并完成出库");
        return data;
    }

    private Optional<ScannedProduct> tryResolveScannedProduct(String scanCode) {
        Optional<CargoCodeRecord> cargoCodeRecord = cargoCodeRecordRepository.findFirstByCargoCodeOrderByIdDesc(scanCode)
                .or(() -> cargoCodeRecordRepository.findFirstByRawContentOrderByIdDesc(scanCode));
        if (cargoCodeRecord.isPresent()) {
            CargoCodeRecord record = cargoCodeRecord.get();
            InboundOrderItem item = inboundOrderItemRepository.findById(record.getInboundOrderItemId())
                    .orElseThrow(() -> new BusinessException("货物码已识别，但入库明细不存在: " + scanCode));
            Product product = productRepository.findById(item.getProductId())
                    .orElseGet(() -> productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(item.getSkuCode(), item.getSkuCode())
                            .orElseThrow(() -> new BusinessException("货物码已识别，但商品档案不存在: " + scanCode)));
            return Optional.of(new ScannedProduct(product, "CARGO_CODE", record.getCargoCode()));
        }

        Optional<InboundOrderItem> inboundItem = inboundOrderItemRepository.findFirstByCargoCodeOrExternalCodeOrderByIdDesc(scanCode, scanCode)
                .or(() -> inboundOrderItemRepository.findFirstByCargoCodeContentOrderByIdDesc(scanCode));
        if (inboundItem.isPresent()) {
            InboundOrderItem item = inboundItem.get();
            Product product = productRepository.findById(item.getProductId())
                    .orElseGet(() -> productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(item.getSkuCode(), item.getSkuCode())
                            .orElseThrow(() -> new BusinessException("货物码已识别，但商品档案不存在: " + scanCode)));
            return Optional.of(new ScannedProduct(product, "CARGO_CODE", scanCode));
        }

        return productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(scanCode, scanCode)
                .map(product -> new ScannedProduct(product, "PRODUCT", scanCode));
    }

    private Optional<ScannedInboundCargo> tryResolveScannedInboundCargo(String scanCode) {
        Optional<CargoCodeRecord> cargoCodeRecord = cargoCodeRecordRepository.findFirstByCargoCodeOrderByIdDesc(scanCode)
                .or(() -> cargoCodeRecordRepository.findFirstByRawContentOrderByIdDesc(scanCode));
        if (cargoCodeRecord.isPresent()) {
            CargoCodeRecord record = cargoCodeRecord.get();
            InboundOrderItem item = inboundOrderItemRepository.findById(record.getInboundOrderItemId())
                    .orElseThrow(() -> new BusinessException("货物码已识别，但入库明细不存在: " + scanCode));
            Product product = resolveProduct(item, scanCode);
            return Optional.of(new ScannedInboundCargo(item, product, "CARGO_CODE", record.getCargoCode()));
        }

        Optional<InboundOrderItem> inboundItem = inboundOrderItemRepository.findFirstByCargoCodeOrExternalCodeOrderByIdDesc(scanCode, scanCode)
                .or(() -> inboundOrderItemRepository.findFirstByCargoCodeContentOrderByIdDesc(scanCode));
        if (inboundItem.isPresent()) {
            InboundOrderItem item = inboundItem.get();
            Product product = resolveProduct(item, scanCode);
            return Optional.of(new ScannedInboundCargo(item, product, "CARGO_CODE", scanCode));
        }

        return productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(scanCode, scanCode)
                .flatMap(product -> inboundOrderItemRepository
                        .findByProductIdAndPutawayScanConfirmedTrueAndOutboundOrderIdIsNullOrderByIdDesc(product.getId())
                        .stream()
                        .findFirst()
                        .map(item -> new ScannedInboundCargo(item, product, "PRODUCT", scanCode)));
    }

    private Product resolveProduct(InboundOrderItem item, String scanCode) {
        return productRepository.findById(item.getProductId())
                .orElseGet(() -> productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(item.getSkuCode(), item.getSkuCode())
                        .orElseThrow(() -> new BusinessException("货物码已识别，但商品档案不存在: " + scanCode)));
    }

    private BigDecimal transferQuantity(InboundOrderItem item) {
        BigDecimal quantity = defaultQty(item.getQualifiedQty());
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = defaultQty(item.getActualQty());
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = defaultQty(item.getExpectedQty());
        }
        return quantity.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : quantity;
    }

    private List<OutboundOrderItem> saveItems(OutboundOrder order, List<OutboundOrderItemRequest> items) {
        return items.stream().map(itemRequest -> {
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
            return outboundOrderItemRepository.save(item);
        }).toList();
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

    private List<String> extractScanCandidates(String rawContent) {
        String raw = rawContent == null ? "" : rawContent.trim();
        if (raw.isBlank()) {
            throw new BusinessException("扫码内容不能为空");
        }
        List<String> candidates = new ArrayList<>();
        candidates.addAll(parseJsonCodes(raw));
        candidates.addAll(parseUriCodes(raw));
        candidates.addAll(parseKeyValueCodes(raw));
        parseGs1Code(raw).ifPresent(candidates::add);
        candidates.addAll(rawCandidates(raw));
        return normalizeCandidates(candidates);
    }

    private List<String> parseJsonCodes(String raw) {
        if (!(raw.startsWith("{") && raw.endsWith("}"))) {
            return List.of();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(raw, new TypeReference<>() {
            });
            return findProductCodes(payload);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> parseUriCodes(String raw) {
        if (!raw.contains("?") && !raw.contains("://")) {
            return List.of();
        }
        try {
            URI uri = URI.create(raw);
            List<String> candidates = new ArrayList<>();
            Map<String, String> query = splitQuery(uri.getRawQuery());
            PRODUCT_CODE_KEYS.stream()
                    .map(key -> findValueIgnoreCase(query, key))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(candidates::add);
            String path = uri.getPath();
            if (path != null && !path.isBlank()) {
                String[] parts = path.split("/");
                for (int i = parts.length - 1; i >= 0; i--) {
                    String part = urlDecode(parts[i]);
                    if (!part.isBlank()) {
                        candidates.add(part);
                        break;
                    }
                }
            }
            return candidates;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> parseKeyValueCodes(String raw) {
        Matcher matcher = PRODUCT_KEY_VALUE_PATTERN.matcher(raw);
        List<String> candidates = new ArrayList<>();
        while (matcher.find()) {
            candidates.add(matcher.group(2).trim());
        }
        return candidates;
    }

    private Optional<String> parseGs1Code(String raw) {
        Matcher matcher = GS1_GTIN_PATTERN.matcher(raw);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private List<String> findProductCodes(Map<String, Object> payload) {
        List<String> matches = PRODUCT_CODE_KEYS.stream()
                .map(key -> findValueIgnoreCase(payload, key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        List<String> nested = payload.values().stream()
                .filter(Map.class::isInstance)
                .map(value -> (Map<?, ?>) value)
                .map(this::stringKeyMap)
                .flatMap(map -> findProductCodes(map).stream())
                .toList();
        List<String> result = new ArrayList<>();
        result.addAll(matches);
        result.addAll(nested);
        return result;
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

    private List<String> rawCandidates(String raw) {
        Set<String> candidates = new LinkedHashSet<>();
        String cleaned = cleanCandidate(raw);
        if (cleaned != null) {
            candidates.add(cleaned);
        }
        Matcher matcher = EXPRESS_LIKE_PATTERN.matcher(raw == null ? "" : raw);
        while (matcher.find()) {
            String candidate = cleanCandidate(matcher.group(1));
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return new ArrayList<>(candidates);
    }

    private List<String> normalizeCandidates(List<String> candidates) {
        Set<String> result = new LinkedHashSet<>();
        for (String candidate : candidates) {
            String cleaned = cleanCandidate(candidate);
            if (cleaned != null) {
                result.add(cleaned);
            }
        }
        return new ArrayList<>(result);
    }

    private String cleanCandidate(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        while ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        cleaned = cleaned.replaceAll("^[,;，；]+|[,;，；]+$", "");
        return cleaned.isBlank() ? null : cleaned;
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

    private record ScannedProduct(Product product, String codeType, String scanCode) {
    }

    private record ScannedInboundCargo(InboundOrderItem inboundItem, Product product, String codeType, String scanCode) {
    }
}
