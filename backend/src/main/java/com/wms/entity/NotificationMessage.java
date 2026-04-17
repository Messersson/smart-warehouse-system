package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notification_message")
public class NotificationMessage extends BaseEntity {

    @Column(name = "channel_type", nullable = false)
    private String channelType;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "biz_type")
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(name = "message_title", nullable = false)
    private String messageTitle;

    @Column(name = "message_body", nullable = false, length = 4000)
    private String messageBody;

    @Column(name = "send_status", nullable = false)
    private String sendStatus;

    @Column(name = "response_status", nullable = false)
    private String responseStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error")
    private String lastError;
}
