package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.InventoryMovement;
import com.wms.entity.Location;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.Stock;
import com.wms.entity.Warehouse;
import com.wms.repository.InventoryMovementRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.StockRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;

    public List<Map<String, Object>> listStocks(String keyword, Long warehouseId) {
        List<Stock> stocks = stockRepository.findAllByOrderByUpdatedAtDesc();
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, warehouse -> warehouse));
        Map<Long, Location> locationMap = locationRepository.findAll().stream()
                .collect(Collectors.toMap(Location::getId, location -> location));

        return stocks.stream()
                .filter(stock -> warehouseId == null || Objects.equals(stock.getWarehouseId(), warehouseId))
                .filter(stock -> {
                    if (!StringUtils.hasText(keyword)) {
                        return true;
                    }
                    Product product = productMap.get(stock.getProductId());
                    Location location = locationMap.get(stock.getLocationId());
                    String loweredKeyword = keyword.toLowerCase();
                    return (stock.getBatchNo() != null && stock.getBatchNo().toLowerCase().contains(loweredKeyword))
                            || (product != null && product.getSkuCode() != null && product.getSkuCode().toLowerCase().contains(loweredKeyword))
                            || (product != null && product.getProductName() != null && product.getProductName().toLowerCase().contains(loweredKeyword))
                            || (product != null && product.getBarcode() != null && product.getBarcode().toLowerCase().contains(loweredKeyword))
                            || (location != null && location.getLocationCode() != null && location.getLocationCode().toLowerCase().contains(loweredKeyword));
                })
                .map(stock -> {
                    Product product = productMap.get(stock.getProductId());
                    Warehouse warehouse = warehouseMap.get(stock.getWarehouseId());
                    Location location = locationMap.get(stock.getLocationId());
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", stock.getId());
                    item.put("warehouseId", stock.getWarehouseId());
                    item.put("warehouseName", warehouse == null ? null : warehouse.getWarehouseName());
                    item.put("productId", stock.getProductId());
                    item.put("skuCode", product == null ? null : product.getSkuCode());
                    item.put("productName", product == null ? null : product.getProductName());
                    item.put("barcode", product == null ? null : product.getBarcode());
                    item.put("locationId", stock.getLocationId());
                    item.put("locationCode", location == null ? null : location.getLocationCode());
                    item.put("locationName", location == null ? null : location.getLocationName());
                    item.put("batchNo", stock.getBatchNo());
                    item.put("quantity", stock.getQuantity());
                    item.put("lockedQty", stock.getLockedQty());
                    item.put("availableQty", stock.getAvailableQty());
                    item.put("lastMovementAt", stock.getLastMovementAt());
                    item.put("updatedAt", stock.getUpdatedAt());
                    return item;
                })
                .toList();
    }

    public Long resolveInboundLocation(Long warehouseId, BigDecimal requiredQty) {
        List<Location> locations = locationRepository.findByWarehouseIdAndStatusOrderByLocationCodeAsc(warehouseId, "ACTIVE");
        return locations.stream()
                .filter(location -> remainingCapacity(location).compareTo(defaultQty(requiredQty)) >= 0)
                .findFirst()
                .map(Location::getId)
                .orElseThrow(() -> new BusinessException("No available location for warehouseId=" + warehouseId));
    }

    @Transactional
    public void increaseStockFromInbound(InboundOrder order, InboundOrderItem item) {
        BigDecimal changeQty = defaultQty(item.getQualifiedQty());
        if (changeQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (item.getLocationId() == null) {
            throw new BusinessException("Inbound item location is required before putaway");
        }

        List<Stock> candidates = selectCandidateStocks(order.getWarehouseId(), order.getOwnerId(), item.getProductId());
        Stock target = candidates.stream()
                .filter(stock -> Objects.equals(stock.getLocationId(), item.getLocationId()))
                .filter(stock -> Objects.equals(stock.getBatchNo(), item.getBatchNo()))
                .findFirst()
                .orElseGet(() -> createEmptyStock(order.getWarehouseId(), order.getOwnerId(), item.getProductId(), item.getLocationId(), item.getBatchNo()));

        BigDecimal beforeQty = defaultQty(target.getQuantity());
        target.setQuantity(beforeQty.add(changeQty));
        target.setLockedQty(defaultQty(target.getLockedQty()));
        target.setAvailableQty(target.getQuantity().subtract(target.getLockedQty()));
        target.setLastInboundAt(LocalDateTime.now());
        target.setLastMovementAt(LocalDateTime.now());
        Stock saved = stockRepository.save(target);

        applyLocationUsageDelta(target.getLocationId(), changeQty);
        saveMovement(saved, "INBOUND", "INBOUND_ORDER", order.getId(), order.getOrderNo(), beforeQty, changeQty, target.getQuantity(), order.getOperatorName(), item.getRemark());
    }

    @Transactional
    public void decreaseStockForOutbound(OutboundOrder order, OutboundOrderItem item) {
        BigDecimal requiredQty = defaultQty(item.getShippedQty()).compareTo(BigDecimal.ZERO) > 0
                ? defaultQty(item.getShippedQty())
                : defaultQty(item.getPlannedQty());
        if (requiredQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        List<Stock> stocks = selectStocksForConsume(order.getWarehouseId(), order.getOwnerId(), item.getProductId());
        BigDecimal remaining = requiredQty;

        for (Stock stock : stocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal availableQty = defaultQty(stock.getAvailableQty());
            if (availableQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal consumeQty = availableQty.min(remaining);
            BigDecimal beforeQty = defaultQty(stock.getQuantity());
            stock.setQuantity(beforeQty.subtract(consumeQty));
            stock.setAvailableQty(stock.getQuantity().subtract(defaultQty(stock.getLockedQty())));
            stock.setLastOutboundAt(LocalDateTime.now());
            stock.setLastMovementAt(LocalDateTime.now());
            Stock saved = stockRepository.save(stock);

            applyLocationUsageDelta(saved.getLocationId(), consumeQty.negate());
            saveMovement(saved, "OUTBOUND", "OUTBOUND_ORDER", order.getId(), order.getOrderNo(), beforeQty, consumeQty.negate(), stock.getQuantity(), order.getOperatorName(), item.getRemark());
            remaining = remaining.subtract(consumeQty);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Insufficient stock for productId=" + item.getProductId());
        }
    }

    @Transactional
    public void decreaseStockForInboundPickup(InboundOrder order, InboundOrderItem item, String operatorName, String remark) {
        BigDecimal requiredQty = defaultQty(item.getQualifiedQty());
        if (requiredQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        List<Stock> stocks = selectCandidateStocks(order.getWarehouseId(), order.getOwnerId(), item.getProductId()).stream()
                .filter(stock -> Objects.equals(stock.getLocationId(), item.getLocationId()))
                .filter(stock -> Objects.equals(stock.getBatchNo(), item.getBatchNo()))
                .toList();
        BigDecimal remaining = requiredQty;

        for (Stock stock : stocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal availableQty = defaultQty(stock.getAvailableQty());
            if (availableQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal consumeQty = availableQty.min(remaining);
            BigDecimal beforeQty = defaultQty(stock.getQuantity());
            stock.setQuantity(beforeQty.subtract(consumeQty));
            stock.setAvailableQty(stock.getQuantity().subtract(defaultQty(stock.getLockedQty())));
            stock.setLastOutboundAt(LocalDateTime.now());
            stock.setLastMovementAt(LocalDateTime.now());
            Stock saved = stockRepository.save(stock);

            applyLocationUsageDelta(saved.getLocationId(), consumeQty.negate());
            saveMovement(saved, "CUSTOMER_PICKUP", "INBOUND_ORDER", order.getId(), order.getOrderNo(), beforeQty, consumeQty.negate(), stock.getQuantity(), operatorName, remark);
            remaining = remaining.subtract(consumeQty);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Pickup stock is insufficient for inbound order " + order.getOrderNo());
        }
    }

    @Transactional
    public void applyLocationUsageDelta(Long locationId, BigDecimal deltaQty) {
        if (locationId == null || deltaQty == null || deltaQty.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        locationRepository.findById(locationId).ifPresent(location -> {
            BigDecimal next = defaultQty(location.getUsedQty()).add(deltaQty);
            if (next.compareTo(BigDecimal.ZERO) < 0) {
                next = BigDecimal.ZERO;
            }
            BigDecimal capacity = defaultQty(location.getCapacityQty());
            if (capacity.compareTo(BigDecimal.ZERO) > 0 && next.compareTo(capacity) > 0) {
                next = capacity;
            }
            location.setUsedQty(next);
            locationRepository.save(location);
        });
    }

    private List<Stock> selectCandidateStocks(Long warehouseId, Long ownerId, Long productId) {
        if (ownerId != null) {
            return stockRepository.findByWarehouseIdAndOwnerIdAndProductIdOrderByUpdatedAtDesc(warehouseId, ownerId, productId);
        }
        return stockRepository.findByWarehouseIdAndProductIdOrderByUpdatedAtDesc(warehouseId, productId);
    }

    private List<Stock> selectStocksForConsume(Long warehouseId, Long ownerId, Long productId) {
        if (ownerId != null) {
            return stockRepository.findByWarehouseIdAndOwnerIdAndProductIdAndQuantityGreaterThanOrderByLastMovementAtAsc(
                    warehouseId, ownerId, productId, BigDecimal.ZERO
            );
        }
        return stockRepository.findByWarehouseIdAndProductIdAndQuantityGreaterThanOrderByLastMovementAtAsc(
                warehouseId, productId, BigDecimal.ZERO
        );
    }

    private Stock createEmptyStock(Long warehouseId, Long ownerId, Long productId, Long locationId, String batchNo) {
        Stock stock = new Stock();
        stock.setWarehouseId(warehouseId);
        stock.setOwnerId(ownerId);
        stock.setProductId(productId);
        stock.setLocationId(locationId);
        stock.setBatchNo(batchNo);
        stock.setQuantity(BigDecimal.ZERO);
        stock.setLockedQty(BigDecimal.ZERO);
        stock.setAvailableQty(BigDecimal.ZERO);
        return stock;
    }

    private void saveMovement(
            Stock stock,
            String movementType,
            String sourceType,
            Long sourceId,
            String sourceNo,
            BigDecimal beforeQty,
            BigDecimal changeQty,
            BigDecimal afterQty,
            String operatorName,
            String remark
    ) {
        InventoryMovement movement = new InventoryMovement();
        movement.setWarehouseId(stock.getWarehouseId());
        movement.setOwnerId(stock.getOwnerId());
        movement.setProductId(stock.getProductId());
        movement.setLocationId(stock.getLocationId());
        movement.setBatchNo(stock.getBatchNo());
        movement.setMovementType(movementType);
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setSourceNo(sourceNo);
        movement.setBeforeQty(beforeQty);
        movement.setChangeQty(changeQty);
        movement.setAfterQty(afterQty);
        movement.setOperatorName(operatorName);
        movement.setRemark(remark);
        inventoryMovementRepository.save(movement);
    }

    private BigDecimal remainingCapacity(Location location) {
        return defaultQty(location.getCapacityQty()).subtract(defaultQty(location.getUsedQty()));
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
