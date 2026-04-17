package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.entity.InboundOrder;
import com.wms.entity.Location;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Warehouse;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;

    public Map<String, Object> lookup(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("扫描编码不能为空");
        }

        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        Optional<InboundOrder> inbound = inboundOrderRepository.findFirstByOrderNoOrPickupCodeOrScanCodeOrderByIdDesc(code, code, code);
        if (inbound.isPresent()) {
            InboundOrder order = inbound.get();
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "INBOUND_ORDER");
            data.put("id", order.getId());
            data.put("orderNo", order.getOrderNo());
            data.put("warehouseId", order.getWarehouseId());
            data.put("warehouseName", warehouseMap.get(order.getWarehouseId()) == null ? null : warehouseMap.get(order.getWarehouseId()).getWarehouseName());
            data.put("status", order.getStatus());
            data.put("pickupStatus", order.getPickupStatus());
            data.put("pickupCode", order.getPickupCode());
            data.put("scanCode", order.getScanCode());
            data.put("receiverName", order.getReceiverName());
            data.put("receiverPhone", order.getReceiverPhone());
            data.put("pickupDueAt", order.getPickupDueAt());
            return data;
        }

        Optional<OutboundOrder> outbound = outboundOrderRepository.findFirstByOrderNoOrLogisticsNoOrderByIdDesc(code, code);
        if (outbound.isPresent()) {
            OutboundOrder order = outbound.get();
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "OUTBOUND_ORDER");
            data.put("id", order.getId());
            data.put("orderNo", order.getOrderNo());
            data.put("warehouseId", order.getWarehouseId());
            data.put("warehouseName", warehouseMap.get(order.getWarehouseId()) == null ? null : warehouseMap.get(order.getWarehouseId()).getWarehouseName());
            data.put("status", order.getStatus());
            data.put("logisticsNo", order.getLogisticsNo());
            data.put("plannedShipTime", order.getPlannedShipTime());
            return data;
        }

        Optional<Location> location = locationRepository.findFirstByLocationCodeOrderByIdAsc(code);
        if (location.isPresent()) {
            Location item = location.get();
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "LOCATION");
            data.put("id", item.getId());
            data.put("warehouseId", item.getWarehouseId());
            data.put("warehouseName", warehouseMap.get(item.getWarehouseId()) == null ? null : warehouseMap.get(item.getWarehouseId()).getWarehouseName());
            data.put("locationCode", item.getLocationCode());
            data.put("locationName", item.getLocationName());
            data.put("zoneName", item.getZoneName());
            data.put("status", item.getStatus());
            return data;
        }

        throw new BusinessException("未找到对应的扫码对象: " + code);
    }
}
