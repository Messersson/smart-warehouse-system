package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.StockTakeCountItemRequest;
import com.wms.dto.StockTakeCountRequest;
import com.wms.dto.StockTakeCreateRequest;
import com.wms.entity.InventoryMovement;
import com.wms.entity.Owner;
import com.wms.entity.Product;
import com.wms.entity.Stock;
import com.wms.entity.StockTakeItem;
import com.wms.entity.StockTakeOrder;
import com.wms.entity.Warehouse;
import com.wms.repository.InventoryMovementRepository;
import com.wms.repository.OwnerRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.StockRepository;
import com.wms.repository.StockTakeItemRepository;
import com.wms.repository.StockTakeOrderRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTakeService {

    private final StockTakeOrderRepository stockTakeOrderRepository;
    private final StockTakeItemRepository stockTakeItemRepository;
    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final OwnerRepository ownerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final ExceptionService exceptionService;
    private final StockService stockService;

    public List<Map<String, Object>> list() {
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        Map<Long, Owner> ownerMap = ownerRepository.findAll().stream()
                .collect(Collectors.toMap(Owner::getId, Function.identity()));
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return stockTakeOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> toView(order, warehouseMap, ownerMap, productMap))
                .toList();
    }

    @Transactional
    public Map<String, Object> create(StockTakeCreateRequest request) {
        StockTakeOrder order = new StockTakeOrder();
        order.setTakeNo("TAKE" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        order.setWarehouseId(request.getWarehouseId());
        order.setOwnerId(request.getOwnerId());
        order.setTakeType(request.getTakeType() == null || request.getTakeType().isBlank() ? "CYCLE" : request.getTakeType());
        order.setStatus("CREATED");
        order.setPlannedStartTime(request.getPlannedStartTime());
        order.setPlannedEndTime(request.getPlannedEndTime());
        order.setOperatorName(request.getOperatorName() == null || request.getOperatorName().isBlank() ? "System Admin" : request.getOperatorName());
        order.setRemark(request.getRemark());
        StockTakeOrder saved = stockTakeOrderRepository.save(order);

        List<Stock> stocks = stockRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(stock -> request.getWarehouseId() == null || request.getWarehouseId().equals(stock.getWarehouseId()))
                .filter(stock -> request.getOwnerId() == null || request.getOwnerId().equals(stock.getOwnerId()))
                .toList();

        stocks.forEach(stock -> {
            StockTakeItem item = new StockTakeItem();
            item.setTakeOrderId(saved.getId());
            item.setStockId(stock.getId());
            item.setProductId(stock.getProductId());
            item.setSystemQty(defaultQty(stock.getQuantity()));
            item.setActualQty(defaultQty(stock.getQuantity()));
            item.setDiffQty(BigDecimal.ZERO);
            item.setStatus("PENDING");
            item.setRemark(null);
            stockTakeItemRepository.save(item);
        });

        return detail(saved.getId());
    }

    public Map<String, Object> detail(Long id) {
        StockTakeOrder order = stockTakeOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock take order not found"));

        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        Map<Long, Owner> ownerMap = ownerRepository.findAll().stream()
                .collect(Collectors.toMap(Owner::getId, Function.identity()));
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return toView(order, warehouseMap, ownerMap, productMap);
    }

    @Transactional
    public Map<String, Object> count(Long id, StockTakeCountRequest request) {
        stockTakeOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock take order not found"));

        Map<Long, StockTakeCountItemRequest> requestMap = request.getItems() == null ? Map.of() :
                request.getItems().stream().collect(Collectors.toMap(StockTakeCountItemRequest::getId, Function.identity()));

        stockTakeItemRepository.findByTakeOrderIdOrderByIdAsc(id).forEach(item -> {
            StockTakeCountItemRequest payload = requestMap.get(item.getId());
            if (payload == null) {
                return;
            }
            BigDecimal actualQty = defaultQty(payload.getActualQty());
            BigDecimal diffQty = actualQty.subtract(defaultQty(item.getSystemQty()));
            item.setActualQty(actualQty);
            item.setDiffQty(diffQty);
            item.setStatus(diffQty.compareTo(BigDecimal.ZERO) == 0 ? "MATCHED" : "DIFF");
            item.setRemark(payload.getRemark());
            stockTakeItemRepository.save(item);
        });

        StockTakeOrder order = stockTakeOrderRepository.findById(id).orElseThrow();
        order.setStatus("COUNTING");
        stockTakeOrderRepository.save(order);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> finish(Long id) {
        StockTakeOrder order = stockTakeOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock take order not found"));
        List<StockTakeItem> items = stockTakeItemRepository.findByTakeOrderIdOrderByIdAsc(id);
        boolean hasDiff = items.stream().anyMatch(item -> defaultQty(item.getDiffQty()).compareTo(BigDecimal.ZERO) != 0);
        order.setStatus(hasDiff ? "REVIEW_REQUIRED" : "FINISHED");
        order.setFinishedAt(LocalDateTime.now());
        stockTakeOrderRepository.save(order);

        if (hasDiff) {
            items.stream()
                    .filter(item -> defaultQty(item.getDiffQty()).compareTo(BigDecimal.ZERO) != 0)
                    .forEach(item -> exceptionService.createFromStockTake(
                            order.getWarehouseId(),
                            order.getId(),
                            order.getTakeNo(),
                            "Stock take difference for item " + item.getId(),
                            "System qty=" + item.getSystemQty() + ", actual qty=" + item.getActualQty() + ", diff=" + item.getDiffQty(),
                            "MEDIUM",
                            order.getOperatorName()
                    ));
        }
        return detail(id);
    }

    @Transactional
    public Map<String, Object> adjust(Long id) {
        StockTakeOrder order = stockTakeOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock take order not found"));
        List<StockTakeItem> items = stockTakeItemRepository.findByTakeOrderIdOrderByIdAsc(id);
        items.forEach(item -> {
            if (defaultQty(item.getDiffQty()).compareTo(BigDecimal.ZERO) == 0) {
                return;
            }
            Stock stock = stockRepository.findById(item.getStockId())
                    .orElseThrow(() -> new BusinessException("Stock not found"));
            BigDecimal beforeQty = defaultQty(stock.getQuantity());
            stock.setQuantity(defaultQty(item.getActualQty()));
            stock.setAvailableQty(stock.getQuantity().subtract(defaultQty(stock.getLockedQty())));
            stock.setLastMovementAt(LocalDateTime.now());
            stockRepository.save(stock);
            stockService.applyLocationUsageDelta(stock.getLocationId(), stock.getQuantity().subtract(beforeQty));

            InventoryMovement movement = new InventoryMovement();
            movement.setWarehouseId(stock.getWarehouseId());
            movement.setOwnerId(stock.getOwnerId());
            movement.setProductId(stock.getProductId());
            movement.setLocationId(stock.getLocationId());
            movement.setBatchNo(stock.getBatchNo());
            movement.setMovementType("STOCK_TAKE_ADJUST");
            movement.setSourceType("STOCK_TAKE_ORDER");
            movement.setSourceId(order.getId());
            movement.setSourceNo(order.getTakeNo());
            movement.setBeforeQty(beforeQty);
            movement.setChangeQty(stock.getQuantity().subtract(beforeQty));
            movement.setAfterQty(stock.getQuantity());
            movement.setOperatorName(order.getOperatorName());
            movement.setRemark(item.getRemark());
            inventoryMovementRepository.save(movement);

            item.setStatus("ADJUSTED");
            stockTakeItemRepository.save(item);
        });

        order.setStatus("ADJUSTED");
        stockTakeOrderRepository.save(order);
        return detail(id);
    }

    private Map<String, Object> toView(StockTakeOrder order, Map<Long, Warehouse> warehouseMap, Map<Long, Owner> ownerMap, Map<Long, Product> productMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("takeNo", order.getTakeNo());
        item.put("warehouseId", order.getWarehouseId());
        item.put("warehouseName", warehouseMap.get(order.getWarehouseId()) == null ? null : warehouseMap.get(order.getWarehouseId()).getWarehouseName());
        item.put("ownerId", order.getOwnerId());
        item.put("ownerName", order.getOwnerId() == null || ownerMap.get(order.getOwnerId()) == null ? null : ownerMap.get(order.getOwnerId()).getOwnerName());
        item.put("takeType", order.getTakeType());
        item.put("status", order.getStatus());
        item.put("plannedStartTime", order.getPlannedStartTime());
        item.put("plannedEndTime", order.getPlannedEndTime());
        item.put("finishedAt", order.getFinishedAt());
        item.put("operatorName", order.getOperatorName());
        item.put("remark", order.getRemark());
        item.put("items", stockTakeItemRepository.findByTakeOrderIdOrderByIdAsc(order.getId()).stream().map(detail -> {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("id", detail.getId());
            detailMap.put("stockId", detail.getStockId());
            detailMap.put("productId", detail.getProductId());
            detailMap.put("skuCode", productMap.get(detail.getProductId()) == null ? null : productMap.get(detail.getProductId()).getSkuCode());
            detailMap.put("productName", productMap.get(detail.getProductId()) == null ? null : productMap.get(detail.getProductId()).getProductName());
            detailMap.put("barcode", productMap.get(detail.getProductId()) == null ? null : productMap.get(detail.getProductId()).getBarcode());
            detailMap.put("systemQty", detail.getSystemQty());
            detailMap.put("actualQty", detail.getActualQty());
            detailMap.put("diffQty", detail.getDiffQty());
            detailMap.put("status", detail.getStatus());
            detailMap.put("remark", detail.getRemark());
            return detailMap;
        }).toList());
        return item;
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
