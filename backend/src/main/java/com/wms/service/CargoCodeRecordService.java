package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.entity.CargoCodeRecord;
import com.wms.repository.CargoCodeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CargoCodeRecordService {

    private final CargoCodeRecordRepository cargoCodeRecordRepository;

    public List<Map<String, Object>> list(Integer limit) {
        int normalizedLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
        return cargoCodeRecordRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(normalizedLimit)
                .map(this::toView)
                .toList();
    }

    public Map<String, Object> detail(Long id) {
        return cargoCodeRecordRepository.findById(id)
                .map(this::toView)
                .orElseThrow(() -> new BusinessException("码记录不存在"));
    }

    public List<Map<String, Object>> listByInboundOrderItem(Long inboundOrderItemId) {
        return cargoCodeRecordRepository.findByInboundOrderItemIdOrderByIdAsc(inboundOrderItemId).stream()
                .map(this::toView)
                .toList();
    }

    public Map<String, Object> toView(CargoCodeRecord record) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", record.getId());
        data.put("inboundOrderId", record.getInboundOrderId());
        data.put("inboundOrderItemId", record.getInboundOrderItemId());
        data.put("warehouseId", record.getWarehouseId());
        data.put("productId", record.getProductId());
        data.put("operationType", record.getOperationType());
        data.put("operationCode", record.getOperationCode());
        data.put("locationId", record.getLocationId());
        data.put("locationCode", record.getLocationCode());
        data.put("locationName", record.getLocationName());
        data.put("zoneName", record.getZoneName());
        data.put("cargoCode", record.getCargoCode());
        data.put("cargoCodeType", record.getCargoCodeType());
        data.put("rawContent", record.getRawContent());
        data.put("svgContent", record.getSvgContent());
        data.put("renderFormat", record.getRenderFormat());
        data.put("renderWidth", record.getRenderWidth());
        data.put("renderHeight", record.getRenderHeight());
        data.put("externalPlatform", record.getExternalPlatform());
        data.put("externalCode", record.getExternalCode());
        data.put("status", record.getStatus());
        data.put("remark", record.getRemark());
        data.put("createdAt", record.getCreatedAt());
        data.put("updatedAt", record.getUpdatedAt());
        return data;
    }
}
