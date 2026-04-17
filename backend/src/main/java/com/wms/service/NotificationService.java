package com.wms.service;

import com.wms.entity.InboundOrder;
import com.wms.entity.NotificationMessage;
import com.wms.entity.Warehouse;
import com.wms.repository.NotificationMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMessageRepository notificationMessageRepository;

    public List<Map<String, Object>> listMessages() {
        return notificationMessageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(message -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", message.getId());
                    item.put("channelType", message.getChannelType());
                    item.put("receiverName", message.getReceiverName());
                    item.put("receiverPhone", message.getReceiverPhone());
                    item.put("warehouseId", message.getWarehouseId());
                    item.put("bizType", message.getBizType());
                    item.put("bizId", message.getBizId());
                    item.put("messageTitle", message.getMessageTitle());
                    item.put("messageBody", message.getMessageBody());
                    item.put("sendStatus", message.getSendStatus());
                    item.put("responseStatus", message.getResponseStatus());
                    item.put("sentAt", message.getSentAt());
                    item.put("lastError", message.getLastError());
                    item.put("createdAt", message.getCreatedAt());
                    return item;
                })
                .toList();
    }

    @Transactional
    public NotificationMessage queuePickupDwellSms(Warehouse warehouse, InboundOrder order) {
        NotificationMessage message = new NotificationMessage();
        message.setChannelType("SMS");
        message.setReceiverName(defaultText(order.getReceiverName(), "客户"));
        message.setReceiverPhone(order.getReceiverPhone());
        message.setWarehouseId(order.getWarehouseId());
        message.setBizType("INBOUND_ORDER");
        message.setBizId(order.getId());
        message.setMessageTitle("取件超时提醒");
        message.setMessageBody(buildPickupMessage(warehouse, order));
        message.setSendStatus("QUEUED");
        message.setResponseStatus("PENDING_CONFIRMATION");
        return notificationMessageRepository.save(message);
    }

    private String buildPickupMessage(Warehouse warehouse, InboundOrder order) {
        String dueAt = order.getPickupDueAt() == null ? "请尽快处理" : order.getPickupDueAt().toString().replace('T', ' ');
        return String.format(
                "【%s】您的包裹/商品单号 %s 已超过设定滞留时长。请确认是否拒收或延迟取货，截止时间：%s，取件码：%s。",
                defaultText(warehouse.getWarehouseName(), "仓库"),
                defaultText(order.getOrderNo(), "-"),
                dueAt,
                defaultText(order.getPickupCode(), defaultText(order.getScanCode(), "-"))
        );
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
