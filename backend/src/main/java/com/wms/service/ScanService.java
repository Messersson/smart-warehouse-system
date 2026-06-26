package com.wms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.common.BusinessException;
import com.wms.dto.ScanRecordRequest;
import com.wms.entity.CargoCodeRecord;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.InboundOrder;
import com.wms.entity.Location;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Product;
import com.wms.entity.ScanRecord;
import com.wms.entity.Warehouse;
import com.wms.repository.CargoCodeRecordRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.ScanRecordRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScanService {

    private static final List<String> CODE_KEYS = List.of(
            "cargoCode",
            "externalCode",
            "externalNo",
            "externalOrderNo",
            "merchantCode",
            "merchantOrderNo",
            "code",
            "barcode",
            "barCode",
            "bar_code",
            "sku",
            "skuCode",
            "sku_code",
            "productCode",
            "product_code",
            "orderNo",
            "order_no",
            "orderId",
            "order_id",
            "tradeNo",
            "tradeId",
            "tid",
            "packageId",
            "packageNo",
            "packageCode",
            "parcelNo",
            "parcelCode",
            "waybillCode",
            "waybillNo",
            "mailNo",
            "mailno",
            "expressNo",
            "expressCode",
            "logisticsCode",
            "pickupCode",
            "scanCode",
            "logisticsNo",
            "trackingNo",
            "trackingNumber",
            "locationCode",
            "pddOrderSn",
            "pddGoodsId",
            "mtOrderId",
            "meituanOrderId",
            "wmOrderViewId",
            "wmOrderId",
            "tbOrderId",
            "tmallOrderId",
            "flashOrderId"
    );
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)(cargoCode|externalCode|externalNo|externalOrderNo|merchantCode|merchantOrderNo|code|barcode|barCode|bar_code|sku|skuCode|sku_code|productCode|product_code|orderNo|order_no|orderId|order_id|tradeNo|tradeId|tid|packageId|packageNo|packageCode|parcelNo|parcelCode|waybillCode|waybillNo|mailNo|mailno|expressNo|expressCode|logisticsCode|pickupCode|scanCode|logisticsNo|trackingNo|trackingNumber|locationCode|pddOrderSn|pddGoodsId|mtOrderId|meituanOrderId|wmOrderViewId|wmOrderId|tbOrderId|tmallOrderId|flashOrderId|货物码|外部码|商家码|订单号|平台单号|快递单号|运单号|物流单号|包裹号|取件码|商品条码|条形码|商品编码|库位编码)\\s*[:=：]\\s*([^\\s,;|，；]+)"
    );
    private static final Pattern EXPRESS_LIKE_PATTERN = Pattern.compile("\\b([A-Z]{1,6}\\d{6,24}|\\d{10,24})\\b");
    private static final Pattern GS1_GTIN_PATTERN = Pattern.compile("\\(01\\)(\\d{14})");

    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final CargoCodeRecordRepository cargoCodeRecordRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ScanRecordRepository scanRecordRepository;
    private final StockService stockService;
    private final ObjectMapper objectMapper;

    public Map<String, Object> lookup(String code) {
        ParsedScan parsedScan = parseScanContent(code);
        return lookupParsedScan(parsedScan);
    }

    public List<Map<String, Object>> listRecords(Integer limit) {
        int normalizedLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return scanRecordRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(normalizedLimit)
                .map(this::toRecordView)
                .toList();
    }

    public Map<String, Object> saveRecord(ScanRecordRequest request) {
        String rawContent = request == null ? null : request.getRawContent();
        ParsedScan parsedScan = parseScanContent(rawContent);

        Map<String, Object> lookupData = null;
        BusinessException lookupException = null;
        try {
            lookupData = lookupParsedScan(parsedScan);
        } catch (BusinessException exception) {
            lookupException = exception;
        }

        ScanRecord record = new ScanRecord();
        record.setRawContent(parsedScan.rawContent());
        record.setParsedCode(parsedScan.parsedCode());
        record.setScanFormat(defaultText(request == null ? null : request.getScanFormat(), "UNKNOWN"));
        record.setContentFormat(parsedScan.contentFormat());
        record.setCodeType(parsedScan.codeType());
        record.setMerchantPlatform(parsedScan.merchantPlatform());
        record.setSourceDevice(defaultText(request == null ? null : request.getSourceDevice(), "UNKNOWN"));
        record.setScannerInterface(defaultText(request == null ? null : request.getScannerInterface(), "WEB_MANUAL"));
        record.setScannerDeviceId(request == null ? null : request.getScannerDeviceId());
        record.setOperatorName(defaultText(request == null ? null : request.getOperatorName(), "System"));
        record.setRemark(request == null ? null : request.getRemark());
        record.setMatched(lookupData != null);
        if (lookupData != null) {
            record.setEntityType(valueAsString(lookupData.get("entityType")));
            record.setEntityId(valueAsLong(lookupData.get("id")));
            record.setWarehouseId(valueAsLong(lookupData.get("warehouseId")));
        } else {
            record.setEntityType("UNMATCHED");
        }

        ScanRecord saved = scanRecordRepository.save(record);
        Map<String, Object> data = new HashMap<>();
        data.put("record", toRecordView(saved));
        data.put("lookupData", lookupData);
        data.put("matched", lookupData != null);
        data.put("message", lookupData == null && lookupException != null ? lookupException.getMessage() : "扫码记录已保存");
        return data;
    }

    private Map<String, Object> lookupParsedScan(ParsedScan parsedScan) {
        BusinessException lastException = null;
        for (String candidate : parsedScan.candidateCodes()) {
            try {
                Map<String, Object> data = lookupParsedCode(candidate);
                data.put("matchedCode", candidate);
                data.put("candidateCodes", parsedScan.candidateCodes());
                return data;
            } catch (BusinessException exception) {
                lastException = exception;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new BusinessException("扫码编码不能为空");
    }

    private Map<String, Object> lookupParsedCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("扫码编码不能为空");
        }
        String lookupCode = code.trim();

        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        Optional<InboundOrder> inbound = inboundOrderRepository.findFirstByOrderNoOrPickupCodeOrScanCodeOrderByIdDesc(lookupCode, lookupCode, lookupCode);
        if (inbound.isPresent()) {
            InboundOrder order = inbound.get();
            return inboundOrderLookupData(order, warehouseMap, order.getScanCode());
        }

        Optional<CargoCodeRecord> cargoCodeRecord = cargoCodeRecordRepository.findFirstByCargoCodeOrderByIdDesc(lookupCode)
                .or(() -> cargoCodeRecordRepository.findFirstByRawContentOrderByIdDesc(lookupCode));
        if (cargoCodeRecord.isPresent()) {
            CargoCodeRecord record = cargoCodeRecord.get();
            InboundOrder order = inboundOrderRepository.findById(record.getInboundOrderId())
                    .orElseThrow(() -> new BusinessException("未找到货物码绑定的入库单: " + lookupCode));
            InboundOrderItem orderItem = inboundOrderItemRepository.findById(record.getInboundOrderItemId())
                    .orElseThrow(() -> new BusinessException("未找到货物码绑定的入库明细: " + lookupCode));
            Map<String, Object> data = inboundOrderItemLookupData(orderItem, order, warehouseMap);
            data.put("cargoCodeRecordId", record.getId());
            data.put("cargoCode", record.getCargoCode());
            data.put("cargoCodeType", record.getCargoCodeType());
            data.put("cargoCodeContent", record.getRawContent());
            data.put("inboundOrderItemId", record.getInboundOrderItemId());
            data.put("productId", record.getProductId());
            data.put("locationId", record.getLocationId());
            data.put("locationCode", record.getLocationCode());
            data.put("locationName", record.getLocationName());
            data.put("zoneName", record.getZoneName());
            return data;
        }

        Optional<InboundOrderItem> inboundItem = inboundOrderItemRepository.findFirstByCargoCodeOrExternalCodeOrderByIdDesc(lookupCode, lookupCode)
                .or(() -> inboundOrderItemRepository.findFirstByCargoCodeContentOrderByIdDesc(lookupCode));
        if (inboundItem.isPresent()) {
            InboundOrderItem orderItem = inboundItem.get();
            InboundOrder order = inboundOrderRepository.findById(orderItem.getOrderId())
                    .orElseThrow(() -> new BusinessException("未找到入库单明细所属入库单: " + lookupCode));
            return inboundOrderItemLookupData(orderItem, order, warehouseMap);
        }

        Optional<OutboundOrder> outbound = outboundOrderRepository.findFirstByOrderNoOrLogisticsNoOrderByIdDesc(lookupCode, lookupCode);
        if (outbound.isPresent()) {
            OutboundOrder order = outbound.get();
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "OUTBOUND_ORDER");
            data.put("id", order.getId());
            data.put("code", order.getOrderNo());
            data.put("orderNo", order.getOrderNo());
            data.put("warehouseId", order.getWarehouseId());
            data.put("warehouseName", warehouseMap.get(order.getWarehouseId()) == null ? null : warehouseMap.get(order.getWarehouseId()).getWarehouseName());
            data.put("status", order.getStatus());
            data.put("logisticsNo", order.getLogisticsNo());
            data.put("plannedShipTime", order.getPlannedShipTime());
            return data;
        }

        Optional<Location> location = locationRepository.findFirstByLocationCodeOrderByIdAsc(lookupCode);
        if (location.isPresent()) {
            Location item = location.get();
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "LOCATION");
            data.put("id", item.getId());
            data.put("code", item.getLocationCode());
            data.put("warehouseId", item.getWarehouseId());
            data.put("warehouseName", warehouseMap.get(item.getWarehouseId()) == null ? null : warehouseMap.get(item.getWarehouseId()).getWarehouseName());
            data.put("locationCode", item.getLocationCode());
            data.put("locationName", item.getLocationName());
            data.put("zoneName", item.getZoneName());
            data.put("status", item.getStatus());
            return data;
        }

        Optional<Product> product = productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(lookupCode, lookupCode);
        if (product.isPresent()) {
            Product item = product.get();
            List<Map<String, Object>> stocks = stockService.listStocks(lookupCode, null);
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "PRODUCT_STOCK");
            data.put("id", item.getId());
            data.put("code", item.getSkuCode());
            data.put("skuCode", item.getSkuCode());
            data.put("productName", item.getProductName());
            data.put("barcode", item.getBarcode());
            data.put("unitName", item.getUnitName());
            data.put("status", item.getStatus());
            data.put("stockCount", stocks.size());
            data.put("totalQuantity", sum(stocks, "quantity"));
            data.put("totalAvailableQty", sum(stocks, "availableQty"));
            data.put("stocks", stocks);
            return data;
        }

        throw new BusinessException("未找到对应的扫码对象: " + lookupCode);
    }

    private Map<String, Object> inboundOrderLookupData(InboundOrder order, Map<Long, Warehouse> warehouseMap, String scanCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityType", "INBOUND_ORDER");
        data.put("id", order.getId());
        data.put("code", order.getOrderNo());
        data.put("orderNo", order.getOrderNo());
        data.put("warehouseId", order.getWarehouseId());
        data.put("warehouseName", warehouseMap.get(order.getWarehouseId()) == null ? null : warehouseMap.get(order.getWarehouseId()).getWarehouseName());
        data.put("status", order.getStatus());
        data.put("pickupStatus", order.getPickupStatus());
        data.put("pickupCode", order.getPickupCode());
        data.put("scanCode", scanCode);
        data.put("orderScanCode", order.getScanCode());
        data.put("receiverName", order.getReceiverName());
        data.put("receiverPhone", order.getReceiverPhone());
        data.put("pickupDueAt", order.getPickupDueAt());
        return data;
    }

    private Map<String, Object> inboundOrderItemLookupData(InboundOrderItem orderItem, InboundOrder order, Map<Long, Warehouse> warehouseMap) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityType", "INBOUND_ORDER_ITEM");
        data.put("id", orderItem.getId());
        data.put("code", orderItem.getCargoCode());
        data.put("cargoCode", orderItem.getCargoCode());
        data.put("cargoCodeType", orderItem.getCargoCodeType());
        data.put("cargoCodeContent", orderItem.getCargoCodeContent());
        data.put("externalPlatform", orderItem.getExternalPlatform());
        data.put("externalCode", orderItem.getExternalCode());
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("warehouseId", order.getWarehouseId());
        data.put("warehouseName", warehouseMap.get(order.getWarehouseId()) == null ? null : warehouseMap.get(order.getWarehouseId()).getWarehouseName());
        data.put("status", order.getStatus());
        data.put("pickupStatus", order.getPickupStatus());
        data.put("supplierId", order.getSupplierId());
        data.put("productId", orderItem.getProductId());
        data.put("skuCode", orderItem.getSkuCode());
        data.put("productName", orderItem.getProductName());
        data.put("batchNo", orderItem.getBatchNo());
        data.put("expectedQty", orderItem.getExpectedQty());
        data.put("actualQty", orderItem.getActualQty());
        data.put("qualifiedQty", orderItem.getQualifiedQty());
        data.put("locationId", orderItem.getLocationId());
        locationRepository.findById(Optional.ofNullable(orderItem.getLocationId()).orElse(-1L)).ifPresent(location -> {
            data.put("locationCode", location.getLocationCode());
            data.put("locationName", location.getLocationName());
            data.put("zoneName", location.getZoneName());
            data.put("aisleNo", location.getAisleNo());
            data.put("shelfNo", location.getShelfNo());
            data.put("layerNo", location.getLayerNo());
            data.put("binNo", location.getBinNo());
            data.put("locationFullName", locationFullName(location));
        });
        data.put("putawayScanConfirmed", Boolean.TRUE.equals(orderItem.getPutawayScanConfirmed()));
        data.put("putawayScanConfirmedAt", orderItem.getPutawayScanConfirmedAt());
        data.put("putawayScanOperator", orderItem.getPutawayScanOperator());
        data.put("putawayScanRecordId", orderItem.getPutawayScanRecordId());
        data.put("outboundOrderId", orderItem.getOutboundOrderId());
        data.put("outboundOrderNo", orderItem.getOutboundOrderNo());
        data.put("outboundTransferredAt", orderItem.getOutboundTransferredAt());
        return data;
    }

    private ParsedScan parseScanContent(String rawContent) {
        String raw = rawContent == null ? "" : rawContent.trim();
        if (raw.isBlank()) {
            throw new BusinessException("扫码内容不能为空");
        }

        Optional<ParsedScan> json = parseJson(raw);
        if (json.isPresent()) {
            return json.get();
        }

        Optional<ParsedScan> uri = parseUri(raw);
        if (uri.isPresent()) {
            return uri.get();
        }

        Optional<ParsedScan> keyValue = parseKeyValue(raw);
        if (keyValue.isPresent()) {
            return keyValue.get();
        }

        Matcher gs1Matcher = GS1_GTIN_PATTERN.matcher(raw);
        if (gs1Matcher.find()) {
            return parsed(raw, "GS1_AI", "GTIN", detectMerchantPlatform(raw), List.of(gs1Matcher.group(1)));
        }

        return parsed(raw, "PLAIN_TEXT", "RAW", detectMerchantPlatform(raw), candidateCodes(raw));
    }

    private Optional<ParsedScan> parseJson(String raw) {
        if (!(raw.startsWith("{") && raw.endsWith("}"))) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(raw, new TypeReference<>() {
            });
            String platform = detectMerchantPlatform(payload, raw);
            List<CodeMatch> matches = findCodesInMap(payload);
            return matches.stream().findFirst()
                    .map(match -> parsed(raw, "JSON", normalizeKey(match.key()), platform, matches.stream().map(CodeMatch::value).toList()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<ParsedScan> parseUri(String raw) {
        if (!raw.contains("://") && !raw.contains("?")) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(raw);
            Map<String, String> query = splitQuery(uri.getRawQuery());
            List<CodeMatch> queryMatches = findCodesInStringMap(query);
            if (!queryMatches.isEmpty()) {
                CodeMatch match = queryMatches.get(0);
                List<String> candidates = new ArrayList<>(queryMatches.stream().map(CodeMatch::value).toList());
                candidates.addAll(candidateCodes(raw));
                return Optional.of(parsed(raw, "URI", normalizeKey(match.key()), detectMerchantPlatform(raw), candidates));
            }

            String path = uri.getPath();
            if (path != null && !path.isBlank()) {
                String[] parts = path.split("/");
                for (int i = parts.length - 1; i >= 0; i--) {
                    String part = urlDecode(parts[i]);
                    if (!part.isBlank()) {
                        List<String> candidates = new ArrayList<>();
                        candidates.add(part);
                        candidates.addAll(candidateCodes(raw));
                        return Optional.of(parsed(raw, "URI", "PATH", detectMerchantPlatform(raw), candidates));
                    }
                }
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<ParsedScan> parseKeyValue(String raw) {
        Matcher matcher = KEY_VALUE_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return Optional.empty();
        }
        List<CodeMatch> matches = new ArrayList<>();
        do {
            matches.add(new CodeMatch(matcher.group(1), matcher.group(2).trim()));
        } while (matcher.find());
        CodeMatch first = matches.get(0);
        return Optional.of(parsed(raw, "KEY_VALUE", normalizeKey(first.key()), detectMerchantPlatform(raw), matches.stream().map(CodeMatch::value).toList()));
    }

    private List<CodeMatch> findCodesInMap(Map<String, Object> payload) {
        List<CodeMatch> matches = CODE_KEYS.stream()
                .map(key -> findValueIgnoreCase(payload, key).map(value -> new CodeMatch(key, value)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        List<CodeMatch> nested = payload.values().stream()
                .filter(Map.class::isInstance)
                .map(value -> (Map<?, ?>) value)
                .map(this::stringKeyMap)
                .flatMap(map -> findCodesInMap(map).stream())
                .toList();
        List<CodeMatch> all = new ArrayList<>();
        all.addAll(matches);
        all.addAll(nested);
        return dedupeMatches(all);
    }

    private List<CodeMatch> findCodesInStringMap(Map<String, String> payload) {
        return dedupeMatches(CODE_KEYS.stream()
                .map(key -> findValueIgnoreCase(payload, key).map(value -> new CodeMatch(key, value)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList());
    }

    private List<CodeMatch> dedupeMatches(List<CodeMatch> matches) {
        Set<String> seen = new LinkedHashSet<>();
        List<CodeMatch> result = new ArrayList<>();
        for (CodeMatch match : matches) {
            String value = cleanCandidate(match.value());
            if (value != null && seen.add(value)) {
                result.add(new CodeMatch(match.key(), value));
            }
        }
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

    private ParsedScan parsed(String raw, String contentFormat, String codeType, String platform, List<String> candidates) {
        List<String> normalizedCandidates = normalizeCandidates(candidates);
        String parsedCode = normalizedCandidates.isEmpty() ? raw : normalizedCandidates.get(0);
        return new ParsedScan(raw, parsedCode, contentFormat, codeType, platform, normalizedCandidates);
    }

    private List<String> candidateCodes(String raw) {
        Set<String> candidates = new LinkedHashSet<>();
        String cleanedRaw = cleanCandidate(raw);
        if (cleanedRaw != null) {
            candidates.add(cleanedRaw);
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

    private Map<String, Object> toRecordView(ScanRecord record) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", record.getId());
        item.put("rawContent", record.getRawContent());
        item.put("parsedCode", record.getParsedCode());
        item.put("scanFormat", record.getScanFormat());
        item.put("contentFormat", record.getContentFormat());
        item.put("codeType", record.getCodeType());
        item.put("merchantPlatform", record.getMerchantPlatform());
        item.put("matched", record.getMatched());
        item.put("entityType", record.getEntityType());
        item.put("entityId", record.getEntityId());
        item.put("warehouseId", record.getWarehouseId());
        item.put("sourceDevice", record.getSourceDevice());
        item.put("scannerInterface", record.getScannerInterface());
        item.put("scannerDeviceId", record.getScannerDeviceId());
        item.put("operatorName", record.getOperatorName());
        item.put("remark", record.getRemark());
        item.put("createdAt", record.getCreatedAt());
        return item;
    }

    private BigDecimal sum(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(row -> row.get(key))
                .filter(BigDecimal.class::isInstance)
                .map(BigDecimal.class::cast)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeKey(String key) {
        return key == null ? "UNKNOWN" : key.trim().toUpperCase();
    }

    private String detectMerchantPlatform(Map<String, Object> payload, String raw) {
        Optional<String> explicit = findValueIgnoreCase(payload, "platform")
                .or(() -> findValueIgnoreCase(payload, "merchantPlatform"))
                .or(() -> findValueIgnoreCase(payload, "sourcePlatform"))
                .or(() -> findValueIgnoreCase(payload, "source"));
        return explicit.map(this::normalizePlatform).orElseGet(() -> detectMerchantPlatform(raw));
    }

    private String detectMerchantPlatform(String raw) {
        String text = raw == null ? "" : raw.toLowerCase();
        if (text.contains("pinduoduo") || text.contains("yangkeduo") || text.contains("pdd")) {
            return "PINDUODUO";
        }
        if (text.contains("meituan") || text.contains("meituan.com") || text.contains("sankuai") || text.contains("mt_") || text.contains("wmorder")) {
            return "MEITUAN";
        }
        if (text.contains("tmall") || text.contains("天猫")) {
            return "TMALL";
        }
        if (text.contains("taobao") || text.contains("tb.cn") || text.contains("淘")) {
            return text.contains("flash") || text.contains("shangou") || text.contains("闪购") ? "TAOBAO_FLASH" : "TAOBAO";
        }
        if (text.contains("wms_inbound")) {
            return "WMS";
        }
        return null;
    }

    private String normalizePlatform(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "WMS", "WMS_INBOUND" -> "WMS";
            case "PDD", "PINDUODUO" -> "PINDUODUO";
            case "MT", "MEITUAN", "MEITUAN_WAIMAI" -> "MEITUAN";
            case "TB", "TAOBAO" -> "TAOBAO";
            case "TM", "TMALL" -> "TMALL";
            case "TAOBAO_SHANGOU", "TAOBAO_FLASH" -> "TAOBAO_FLASH";
            default -> normalized;
        };
    }

    private String locationFullName(Location location) {
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

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
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

    private record ParsedScan(String rawContent, String parsedCode, String contentFormat, String codeType, String merchantPlatform, List<String> candidateCodes) {
    }

    private record CodeMatch(String key, String value) {
    }
}
