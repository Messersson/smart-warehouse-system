package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.InboundOrderItemRequest;
import com.wms.dto.InboundOrderRequest;
import com.wms.dto.VirtualPlatformOrderItemRequest;
import com.wms.dto.VirtualPlatformOrderRequest;
import com.wms.entity.Product;
import com.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VirtualPlatformService {

    private final ProductRepository productRepository;
    private final InboundService inboundService;

    @Transactional
    public Map<String, Object> createInboundOrder(VirtualPlatformOrderRequest request) {
        InboundOrderRequest inboundRequest = new InboundOrderRequest();
        inboundRequest.setOrderNo(request.getPlatformOrderNo());
        inboundRequest.setWarehouseId(request.getWarehouseId());
        inboundRequest.setSupplierId(request.getSupplierId());
        inboundRequest.setOwnerId(request.getOwnerId());
        inboundRequest.setCustomerId(request.getCustomerId());
        inboundRequest.setOrderType("PLATFORM_SUPPLY");
        inboundRequest.setSourceNo(request.getPlatformOrderNo());
        inboundRequest.setExpectedArrivalTime(request.getExpectedArrivalTime());
        inboundRequest.setOperatorName(defaultText(request.getOperatorName(), "Virtual Platform"));
        inboundRequest.setReceiverName(request.getReceiverName());
        inboundRequest.setReceiverPhone(request.getReceiverPhone());
        inboundRequest.setRemark(request.getRemark());
        inboundRequest.setItems(request.getItems().stream()
                .map(item -> toInboundItem(request, item))
                .toList());
        return inboundService.create(inboundRequest);
    }

    private InboundOrderItemRequest toInboundItem(VirtualPlatformOrderRequest order, VirtualPlatformOrderItemRequest item) {
        Product product = resolveProduct(item);
        InboundOrderItemRequest request = new InboundOrderItemRequest();
        request.setProductId(product.getId());
        request.setBatchNo(item.getBatchNo());
        request.setCargoCodeType(item.getCargoCodeType());
        request.setExternalPlatform(order.getPlatformType());
        request.setExternalCode(defaultText(item.getExternalCode(), order.getPlatformOrderNo()));
        request.setExpectedQty(defaultQty(item.getQuantity()));
        request.setActualQty(defaultQty(item.getQuantity()));
        request.setQualifiedQty(defaultQty(item.getQuantity()));
        request.setLocationId(item.getLocationId());
        request.setProductionDate(item.getProductionDate());
        request.setExpiryDate(item.getExpiryDate());
        request.setRemark(item.getRemark());
        return request;
    }

    private Product resolveProduct(VirtualPlatformOrderItemRequest item) {
        if (item.getProductId() != null) {
            return productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found: " + item.getProductId()));
        }
        String skuCode = firstText(item.getSkuCode(), item.getPlatformSku());
        String barcode = item.getBarcode();
        if (isBlank(skuCode) && isBlank(barcode)) {
            throw new BusinessException("Product id, skuCode or barcode is required");
        }
        return productRepository.findFirstBySkuCodeOrBarcodeOrderByIdAsc(skuCode, barcode)
                .orElseThrow(() -> new BusinessException("Product not found: " + firstText(skuCode, barcode)));
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private String firstText(String first, String second) {
        return !isBlank(first) ? first.trim() : (!isBlank(second) ? second.trim() : null);
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
