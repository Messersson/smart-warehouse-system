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
            migrateSupplierTable();
            migrateInboundOrderTable();
            migrateInboundOrderItemTable();
            migrateCargoCodeRecordTable();
            backfillInboundCargoCodes();
            backfillCargoCodeRecords();
            shortenLegacyBarcodeCargoCodes();
            migrateNotificationTable();
            migrateScanRecordTable();
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

    private void migrateSupplierTable() {
        addColumnIfMissing("base_supplier", "warehouse_id", "BIGINT DEFAULT NULL");
        addColumnIfMissing("base_supplier", "platform_type", "VARCHAR(64) DEFAULT NULL");
        addIndexIfMissing("base_supplier", "idx_supplier_warehouse", "(warehouse_id)");
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

    private void migrateInboundOrderItemTable() {
        addColumnIfMissing("inbound_order_item", "cargo_code", "VARCHAR(128) DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "cargo_code_type", "VARCHAR(32) NOT NULL DEFAULT 'QR_CODE'");
        addColumnIfMissing("inbound_order_item", "cargo_code_content", "VARCHAR(2048) DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "external_platform", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "external_code", "VARCHAR(255) DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "putaway_scan_confirmed", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("inbound_order_item", "putaway_scan_confirmed_at", "DATETIME DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "putaway_scan_operator", "VARCHAR(128) DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "putaway_scan_record_id", "BIGINT DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "outbound_order_id", "BIGINT DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "outbound_order_no", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("inbound_order_item", "outbound_transferred_at", "DATETIME DEFAULT NULL");
        addIndexIfMissing("inbound_order_item", "idx_inbound_item_cargo_code", "(cargo_code)");
        addIndexIfMissing("inbound_order_item", "idx_inbound_item_external_code", "(external_code)");
        addIndexIfMissing("inbound_order_item", "idx_inbound_item_cargo_content", "(cargo_code_content(255))");
        addIndexIfMissing("inbound_order_item", "idx_inbound_item_putaway_scan", "(putaway_scan_confirmed)");
        addIndexIfMissing("inbound_order_item", "idx_inbound_item_outbound", "(outbound_order_id)");
    }

    private void migrateCargoCodeRecordTable() {
        createTableIfMissing(
                "cargo_code_record",
                """
                        CREATE TABLE cargo_code_record (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          inbound_order_id BIGINT NOT NULL,
                          inbound_order_item_id BIGINT NOT NULL,
                          warehouse_id BIGINT DEFAULT NULL,
                          product_id BIGINT DEFAULT NULL,
                          operation_type VARCHAR(64) DEFAULT NULL,
                          operation_code VARCHAR(64) DEFAULT NULL,
                          location_id BIGINT DEFAULT NULL,
                          location_code VARCHAR(128) DEFAULT NULL,
                          location_name VARCHAR(128) DEFAULT NULL,
                          zone_name VARCHAR(128) DEFAULT NULL,
                          cargo_code VARCHAR(128) NOT NULL,
                          cargo_code_type VARCHAR(32) NOT NULL,
                          raw_content TEXT NOT NULL,
                          svg_content MEDIUMTEXT DEFAULT NULL,
                          render_format VARCHAR(64) DEFAULT NULL,
                          render_width INT DEFAULT NULL,
                          render_height INT DEFAULT NULL,
                          external_platform VARCHAR(64) DEFAULT NULL,
                          external_code VARCHAR(255) DEFAULT NULL,
                          status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                          remark VARCHAR(255) DEFAULT NULL,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          KEY idx_cargo_code_record_order (inbound_order_id),
                          KEY idx_cargo_code_record_item (inbound_order_item_id),
                          KEY idx_cargo_code_record_code (cargo_code),
                          KEY idx_cargo_code_record_operation (operation_code),
                          KEY idx_cargo_code_record_location (location_code)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """
        );
        addColumnIfMissing("cargo_code_record", "operation_type", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("cargo_code_record", "operation_code", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("cargo_code_record", "location_id", "BIGINT DEFAULT NULL");
        addColumnIfMissing("cargo_code_record", "location_code", "VARCHAR(128) DEFAULT NULL");
        addColumnIfMissing("cargo_code_record", "location_name", "VARCHAR(128) DEFAULT NULL");
        addColumnIfMissing("cargo_code_record", "zone_name", "VARCHAR(128) DEFAULT NULL");
        addIndexIfMissing("cargo_code_record", "idx_cargo_code_record_operation", "(operation_code)");
        addIndexIfMissing("cargo_code_record", "idx_cargo_code_record_location", "(location_code)");
    }

    private void backfillInboundCargoCodes() {
        jdbcTemplate.execute("""
                UPDATE inbound_order_item
                SET cargo_code = CONCAT('G', LPAD(UPPER(CONV(id, 10, 36)), 7, '0'))
                WHERE cargo_code IS NULL OR cargo_code = ''
                """);
        jdbcTemplate.execute("""
                UPDATE inbound_order_item
                SET cargo_code_type = 'BAR_CODE'
                WHERE cargo_code_type IS NULL OR cargo_code_type = ''
                """);
        jdbcTemplate.execute("""
                UPDATE inbound_order_item
                SET cargo_code_content = cargo_code
                WHERE cargo_code IS NOT NULL
                  AND cargo_code <> ''
                  AND (cargo_code_content IS NULL OR cargo_code_content = '')
                """);
    }

    private void shortenLegacyBarcodeCargoCodes() {
        jdbcTemplate.execute("""
                UPDATE cargo_code_record
                JOIN inbound_order_item
                  ON inbound_order_item.id = cargo_code_record.inbound_order_item_id
                SET cargo_code_record.cargo_code = CONCAT('G', LPAD(UPPER(CONV(inbound_order_item.id, 10, 36)), 7, '0')),
                    cargo_code_record.raw_content = CONCAT('G', LPAD(UPPER(CONV(inbound_order_item.id, 10, 36)), 7, '0')),
                    cargo_code_record.svg_content = NULL,
                    cargo_code_record.render_format = NULL,
                    cargo_code_record.render_width = NULL,
                    cargo_code_record.render_height = NULL
                WHERE inbound_order_item.cargo_code REGEXP '^WMSG[0-9]{10,}'
                  AND inbound_order_item.cargo_code_type = 'BAR_CODE'
                  AND inbound_order_item.outbound_order_id IS NULL
                """);
        jdbcTemplate.execute("""
                UPDATE inbound_order_item
                SET cargo_code = CONCAT('G', LPAD(UPPER(CONV(id, 10, 36)), 7, '0')),
                    cargo_code_content = CONCAT('G', LPAD(UPPER(CONV(id, 10, 36)), 7, '0'))
                WHERE cargo_code REGEXP '^WMSG[0-9]{10,}'
                  AND cargo_code_type = 'BAR_CODE'
                  AND outbound_order_id IS NULL
                """);
    }

    private void backfillCargoCodeRecords() {
        jdbcTemplate.execute("""
                INSERT INTO cargo_code_record (
                  inbound_order_id,
                  inbound_order_item_id,
                  warehouse_id,
                  product_id,
                  operation_type,
                  operation_code,
                  location_id,
                  location_code,
                  location_name,
                  zone_name,
                  cargo_code,
                  cargo_code_type,
                  raw_content,
                  status,
                  remark,
                  created_at,
                  updated_at
                )
                SELECT
                  inbound_order.id,
                  inbound_order_item.id,
                  inbound_order.warehouse_id,
                  inbound_order_item.product_id,
                  'INBOUND_PUTAWAY',
                  inbound_order.order_no,
                  inbound_order_item.location_id,
                  base_location.location_code,
                  base_location.location_name,
                  base_location.zone_name,
                  inbound_order_item.cargo_code,
                  COALESCE(NULLIF(inbound_order_item.cargo_code_type, ''), 'BAR_CODE'),
                  COALESCE(NULLIF(inbound_order_item.cargo_code_content, ''), inbound_order_item.cargo_code),
                  'ACTIVE',
                  '迁移补齐入库货物码',
                  NOW(),
                  NOW()
                FROM inbound_order_item
                JOIN inbound_order ON inbound_order.id = inbound_order_item.order_id
                LEFT JOIN base_location ON base_location.id = inbound_order_item.location_id
                LEFT JOIN cargo_code_record existing_record
                  ON existing_record.inbound_order_item_id = inbound_order_item.id
                 AND existing_record.cargo_code = inbound_order_item.cargo_code
                WHERE inbound_order_item.cargo_code IS NOT NULL
                  AND inbound_order_item.cargo_code <> ''
                  AND existing_record.id IS NULL
                """);
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

    private void migrateScanRecordTable() {
        createTableIfMissing(
                "scan_record",
                """
                        CREATE TABLE scan_record (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          raw_content TEXT NOT NULL,
                          parsed_code VARCHAR(255) DEFAULT NULL,
                          scan_format VARCHAR(64) DEFAULT NULL,
                          content_format VARCHAR(64) DEFAULT NULL,
                          code_type VARCHAR(64) DEFAULT NULL,
                          merchant_platform VARCHAR(64) DEFAULT NULL,
                          matched TINYINT(1) NOT NULL DEFAULT 0,
                          entity_type VARCHAR(64) DEFAULT NULL,
                          entity_id BIGINT DEFAULT NULL,
                          warehouse_id BIGINT DEFAULT NULL,
                          source_device VARCHAR(64) DEFAULT NULL,
                          scanner_interface VARCHAR(64) DEFAULT NULL,
                          scanner_device_id VARCHAR(128) DEFAULT NULL,
                          operator_name VARCHAR(128) DEFAULT NULL,
                          remark VARCHAR(255) DEFAULT NULL,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          KEY idx_scan_record_created_at (created_at),
                          KEY idx_scan_record_parsed_code (parsed_code),
                          KEY idx_scan_record_entity (entity_type, entity_id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """
        );
        addColumnIfMissing("scan_record", "merchant_platform", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("scan_record", "scanner_interface", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("scan_record", "scanner_device_id", "VARCHAR(128) DEFAULT NULL");
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

    private void createTableIfMissing(String tableName, String sql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        if (count != null && count == 0) {
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
