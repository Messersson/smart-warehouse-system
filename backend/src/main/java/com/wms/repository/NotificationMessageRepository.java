package com.wms.repository;

import com.wms.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {

    java.util.List<NotificationMessage> findAllByOrderByCreatedAtDesc();

    Optional<NotificationMessage> findFirstByBizTypeAndBizIdAndChannelTypeOrderByIdDesc(String bizType, Long bizId, String channelType);
}
