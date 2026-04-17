package com.wms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public ApplicationRunner schemaMigrationApplicationRunner() {
        return args -> {
            migrateWarehouseTable();
            migrateInboundOrderTable();
            migrateNotificationTable();
        };
    }

    private void migrateWarehouseTable() {
        addColumnIfMissing("base_warehouse", "scene_type", "VARCHAR(32) NOT NULL DEFAULT 'GENERAL_STORAGE'");
        addColumnIfMissing("base_warehouse", "dwell_alert_minutes", "INT NOT NULL DEFAULT 0");
        addColumnIfMissing("base_warehouse", "sms_notify_enabled", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("base_warehouse", "sms_reminder_interval_minutes", "INT NOT NULL DEFAULT 120");
        addColumnIfMissing("base_warehouse", "auto_assign_location", "TINYINT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("base_warehouse", "scan_mode", "VARCHAR(32) NOT NULL DEFAULT 'QR_CODE'");
    }

    private void migrateInboundOrderTable() {
        addColumnIfMissing("inbound_order", "customer_id", "BIGINT DEFAULT NULL");
        addColumnIfMissing("inbound_order", "receiver_name", "VARCHAR(128) DEFAULT NULL");
        addColumnIfMissing("inbound_order", "receiver_phone", "VARCHAR(32) DEFAULT NULL");
        addColumnIfMissing("inbound_order", "pickup_status", "VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED'");
        addColumnIfMissing("inbound_order", "pickup_code", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("inbound_order", "pickup_due_at", "DATETIME DEFAULT NULL");
        addColumnIfMissing("inbound_order", "last_sms_notice_at", "DATETIME DEFAULT NULL");
        addColumnIfMissing("inbound_order", "scan_code", "VARCHAR(64) DEFAULT NULL");
        addIndexIfMissing("inbound_order", "idx_inbound_pickup_status", "(pickup_status, pickup_due_at)");
    }

    private void migrateNotificationTable() {
        addColumnIfMissing("notification_message", "receiver_phone", "VARCHAR(32) DEFAULT NULL");
        addColumnIfMissing("notification_message", "warehouse_id", "BIGINT DEFAULT NULL");
        addColumnIfMissing("notification_message", "biz_type", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("notification_message", "biz_id", "BIGINT DEFAULT NULL");
        addColumnIfMissing("notification_message", "response_status", "VARCHAR(32) NOT NULL DEFAULT 'NONE'");
        addColumnIfMissing("notification_message", "last_error", "TEXT DEFAULT NULL");
        addColumnIfMissing("notification_message", "updated_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        addIndexIfMissing("notification_message", "idx_notification_biz", "(biz_type, biz_id)");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition;
            log.info("Applying schema migration: {}", sql);
            jdbcTemplate.execute(sql);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count != null && count == 0) {
            String sql = "ALTER TABLE " + tableName + " ADD INDEX " + indexName + " " + definition;
            log.info("Applying schema migration: {}", sql);
            jdbcTemplate.execute(sql);
        }
    }
}
