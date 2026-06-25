-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: warehouse_system
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alert_event`
--

DROP TABLE IF EXISTS `alert_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `alert_code` varchar(128) NOT NULL,
  `rule_code` varchar(64) NOT NULL,
  `rule_name` varchar(128) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint DEFAULT NULL,
  `severity` varchar(32) NOT NULL DEFAULT 'MEDIUM',
  `alert_message` varchar(500) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'OPEN',
  `first_triggered_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_triggered_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `acknowledged_by` varchar(128) DEFAULT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_event_status` (`status`),
  KEY `idx_alert_event_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert_event`
--

LOCK TABLES `alert_event` WRITE;
/*!40000 ALTER TABLE `alert_event` DISABLE KEYS */;
INSERT INTO `alert_event` VALUES (1,'INBOUND_PUTAWAY_TIMEOUT:INBOUND_ORDER:1','INBOUND_PUTAWAY_TIMEOUT','上架滞留预警','INBOUND_ORDER',1,'HIGH','入库单 IN202604150001 收货后超过 120 分钟仍未上架','RESOLVED','2026-05-16 18:03:20','2026-05-16 19:16:34','系统管理员','2026-05-16 19:16:08','2026-05-16 19:16:34','2026-05-16 18:03:20','2026-05-16 19:16:34'),(2,'OUTBOUND_SHIP_TIMEOUT:OUTBOUND_ORDER:1','OUTBOUND_SHIP_TIMEOUT','出库超时预警','OUTBOUND_ORDER',1,'HIGH','出库单 OUT202604150001 已超过计划发运时限 60 分钟','RESOLVED','2026-05-16 18:03:20','2026-05-24 13:34:06','系统管理员','2026-05-16 19:16:09','2026-05-24 13:34:06','2026-05-16 18:03:20','2026-05-24 13:34:06'),(3,'STOCK_STAGNANT:INVENTORY_STOCK:1','STOCK_STAGNANT','呆滞库存预警','INVENTORY_STOCK',1,'MEDIUM','库存 矿泉水 550ml 已超过 30 天无库存变动','RESOLVED','2026-05-16 18:03:20','2026-05-24 13:34:06','系统管理员','2026-05-16 19:16:10','2026-05-24 13:34:06','2026-05-16 18:03:20','2026-05-24 13:34:06'),(4,'STOCK_STAGNANT:INVENTORY_STOCK:2','STOCK_STAGNANT','呆滞库存预警','INVENTORY_STOCK',2,'MEDIUM','库存 冷藏牛奶 250ml 已超过 30 天无库存变动','OPEN','2026-06-05 09:56:57','2026-06-06 21:13:49',NULL,NULL,NULL,'2026-06-05 09:56:57','2026-06-06 21:13:49');
/*!40000 ALTER TABLE `alert_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alert_rule`
--

DROP TABLE IF EXISTS `alert_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(64) NOT NULL,
  `rule_name` varchar(128) NOT NULL,
  `rule_type` varchar(64) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `threshold_value` int NOT NULL DEFAULT '0',
  `threshold_unit` varchar(32) NOT NULL DEFAULT 'MINUTE',
  `severity` varchar(32) NOT NULL DEFAULT 'MEDIUM',
  `notify_to` varchar(255) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_code` (`rule_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert_rule`
--

LOCK TABLES `alert_rule` WRITE;
/*!40000 ALTER TABLE `alert_rule` DISABLE KEYS */;
INSERT INTO `alert_rule` VALUES (1,'INBOUND_RECEIVE_TIMEOUT','收货超时预警','TIMEOUT','INBOUND_ORDER',120,'MINUTE','MEDIUM','仓库主管',1,'预计到仓后超过120分钟未收货','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,'INBOUND_PUTAWAY_TIMEOUT','上架滞留预警','TIMEOUT','INBOUND_ORDER',120,'MINUTE','HIGH','仓库主管,仓库经理',1,'收货后超过120分钟未上架','2026-05-16 09:53:01','2026-05-16 09:53:01'),(3,'OUTBOUND_SHIP_TIMEOUT','出库超时预警','TIMEOUT','OUTBOUND_ORDER',60,'MINUTE','HIGH','仓库主管,客服经理',1,'计划发货时间后60分钟未发出','2026-05-16 09:53:01','2026-05-16 09:53:01'),(4,'STOCK_STAGNANT','呆滞库存预警','AGING','INVENTORY_STOCK',30,'DAY','MEDIUM','仓库主管,销售经理',1,'库存超过30天无变动','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `alert_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_order`
--

DROP TABLE IF EXISTS `approval_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approval_no` varchar(64) NOT NULL,
  `approval_type` varchar(64) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint NOT NULL,
  `biz_no` varchar(64) DEFAULT NULL,
  `applicant_name` varchar(128) DEFAULT NULL,
  `approver_name` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `current_node` varchar(64) DEFAULT NULL,
  `apply_reason` varchar(255) DEFAULT NULL,
  `approval_comment` varchar(255) DEFAULT NULL,
  `applied_at` datetime DEFAULT NULL,
  `decided_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `approval_no` (`approval_no`),
  KEY `idx_approval_order_status` (`status`),
  KEY `idx_approval_order_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_order`
--

LOCK TABLES `approval_order` WRITE;
/*!40000 ALTER TABLE `approval_order` DISABLE KEYS */;
INSERT INTO `approval_order` VALUES (1,'AP202604150001','ORDER_APPROVAL','OUTBOUND_ORDER',1,'OUT202604150001','系统管理员','仓库主管','PENDING','MANAGER_REVIEW','加急订单需要主管确认',NULL,'2026-05-16 09:53:01',NULL,'2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `approval_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_record`
--

DROP TABLE IF EXISTS `approval_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approval_order_id` bigint NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `operator_name` varchar(128) DEFAULT NULL,
  `action_comment` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_record`
--

LOCK TABLES `approval_record` WRITE;
/*!40000 ALTER TABLE `approval_record` DISABLE KEYS */;
INSERT INTO `approval_record` VALUES (1,1,'SUBMIT','系统管理员','提交出库单审批','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `approval_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_name` varchar(64) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint DEFAULT NULL,
  `action_type` varchar(64) NOT NULL,
  `operator_name` varchar(128) DEFAULT NULL,
  `request_ip` varchar(64) DEFAULT NULL,
  `change_snapshot` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_carrier`
--

DROP TABLE IF EXISTS `base_carrier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_carrier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `carrier_code` varchar(64) NOT NULL,
  `carrier_name` varchar(128) NOT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `service_level` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `carrier_code` (`carrier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_carrier`
--

LOCK TABLES `base_carrier` WRITE;
/*!40000 ALTER TABLE `base_carrier` DISABLE KEYS */;
INSERT INTO `base_carrier` VALUES (1,'CAR-001','顺达物流','孙调度','13900000005','次日达','ACTIVE','默认承运商','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `base_carrier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_customer`
--

DROP TABLE IF EXISTS `base_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_code` varchar(64) NOT NULL,
  `customer_name` varchar(128) NOT NULL,
  `customer_type` varchar(64) NOT NULL DEFAULT 'B2B',
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `customer_code` (`customer_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_customer`
--

LOCK TABLES `base_customer` WRITE;
/*!40000 ALTER TABLE `base_customer` DISABLE KEYS */;
INSERT INTO `base_customer` VALUES (1,'CUS-001','重点客户A','B2B','赵经理','13900000004','customer@example.com','浙江杭州','ACTIVE','默认客户','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `base_customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_location`
--

DROP TABLE IF EXISTS `base_location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_location` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `zone_name` varchar(128) DEFAULT NULL,
  `location_code` varchar(64) NOT NULL,
  `location_name` varchar(128) NOT NULL,
  `aisle_no` varchar(32) DEFAULT NULL,
  `shelf_no` varchar(32) DEFAULT NULL,
  `layer_no` varchar(32) DEFAULT NULL,
  `bin_no` varchar(32) DEFAULT NULL,
  `capacity_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `used_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `pickable` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_location_code` (`warehouse_id`,`location_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_location`
--

LOCK TABLES `base_location` WRITE;
/*!40000 ALTER TABLE `base_location` DISABLE KEYS */;
INSERT INTO `base_location` VALUES (1,1,'收货区','A-01-01-01','A排1架1层1位','A','01','01','01',1000.00,100.00,'ACTIVE',1,'默认库位','2026-05-16 09:53:01','2026-05-24 13:30:05'),(2,1,'拣选区','B-01-01-01','B排1架1层1位','B','01','01','01',1000.00,0.00,'ACTIVE',1,'默认拣选位','2026-05-16 09:53:01','2026-05-16 09:53:01'),(3,1,'冷藏区','C-01-01-01','C排1架1层1位','C','01','01','01',500.00,0.00,'ACTIVE',1,'默认冷藏位','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `base_location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_owner`
--

DROP TABLE IF EXISTS `base_owner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_owner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_code` varchar(64) NOT NULL,
  `owner_name` varchar(128) NOT NULL,
  `owner_type` varchar(64) NOT NULL DEFAULT 'SELF',
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `owner_code` (`owner_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_owner`
--

LOCK TABLES `base_owner` WRITE;
/*!40000 ALTER TABLE `base_owner` DISABLE KEYS */;
INSERT INTO `base_owner` VALUES (1,'OWN-001','自营货主','SELF','李经理','13900000002','owner@example.com','上海浦东新区','ACTIVE','默认货主','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `base_owner` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_product`
--

DROP TABLE IF EXISTS `base_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_code` varchar(64) NOT NULL,
  `product_name` varchar(128) NOT NULL,
  `product_spec` varchar(128) DEFAULT NULL,
  `category_name` varchar(128) DEFAULT NULL,
  `brand_name` varchar(128) DEFAULT NULL,
  `unit_name` varchar(32) NOT NULL DEFAULT '件',
  `barcode` varchar(64) DEFAULT NULL,
  `safe_stock` decimal(18,2) NOT NULL DEFAULT '0.00',
  `max_stock` decimal(18,2) NOT NULL DEFAULT '0.00',
  `shelf_life_days` int NOT NULL DEFAULT '0',
  `enable_batch` tinyint(1) NOT NULL DEFAULT '1',
  `enable_serial` tinyint(1) NOT NULL DEFAULT '0',
  `weight_kg` decimal(18,3) NOT NULL DEFAULT '0.000',
  `volume_m3` decimal(18,4) NOT NULL DEFAULT '0.0000',
  `sale_price` decimal(18,2) NOT NULL DEFAULT '0.00',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sku_code` (`sku_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_product`
--

LOCK TABLES `base_product` WRITE;
/*!40000 ALTER TABLE `base_product` DISABLE KEYS */;
INSERT INTO `base_product` VALUES (1,'SKU-001','矿泉水 550ml','24瓶/箱','日用品','清泉','箱','690000000001',20.00,5000.00,365,1,0,12.000,0.0450,36.00,'ACTIVE','默认商品','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,'SKU-002','冷藏牛奶 250ml','12盒/箱','冷链食品','牧场','箱','690000000002',10.00,2000.00,30,1,0,6.000,0.0220,58.00,'ACTIVE','默认商品','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `base_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_product_category`
--

DROP TABLE IF EXISTS `base_product_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(64) NOT NULL,
  `category_name` varchar(128) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_product_category`
--

LOCK TABLES `base_product_category` WRITE;
/*!40000 ALTER TABLE `base_product_category` DISABLE KEYS */;
INSERT INTO `base_product_category` VALUES (1,'CAT-001','日用品',NULL,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,'CAT-002','冷链食品',NULL,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `base_product_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_supplier`
--

DROP TABLE IF EXISTS `base_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_code` varchar(64) NOT NULL,
  `supplier_name` varchar(128) NOT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `warehouse_id` bigint DEFAULT NULL,
  `platform_type` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `supplier_code` (`supplier_code`),
  KEY `idx_supplier_warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_supplier`
--

LOCK TABLES `base_supplier` WRITE;
/*!40000 ALTER TABLE `base_supplier` DISABLE KEYS */;
INSERT INTO `base_supplier` VALUES (1,'SUP-001','华东供应商','王采购','13900000003','supplier@example.com','江苏昆山','ACTIVE','默认供应商','2026-05-16 09:53:01','2026-05-16 09:53:01',NULL,NULL);
/*!40000 ALTER TABLE `base_supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_warehouse`
--

DROP TABLE IF EXISTS `base_warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_code` varchar(64) NOT NULL,
  `warehouse_name` varchar(128) NOT NULL,
  `warehouse_type` varchar(64) NOT NULL DEFAULT 'GENERAL',
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `province` varchar(64) DEFAULT NULL,
  `city` varchar(64) DEFAULT NULL,
  `district` varchar(64) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `scene_type` varchar(32) NOT NULL DEFAULT 'GENERAL_STORAGE',
  `dwell_alert_minutes` int NOT NULL DEFAULT '0',
  `sms_notify_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `sms_reminder_interval_minutes` int NOT NULL DEFAULT '120',
  `auto_assign_location` tinyint(1) NOT NULL DEFAULT '1',
  `scan_mode` varchar(32) NOT NULL DEFAULT 'QR_CODE',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_warehouse`
--

LOCK TABLES `base_warehouse` WRITE;
/*!40000 ALTER TABLE `base_warehouse` DISABLE KEYS */;
INSERT INTO `base_warehouse` VALUES (1,'WH-SH-001','上海中心仓','GENERAL','张主管','13900000001','上海','上海','浦东新区','临港新片区仓储大道100号','ACTIVE','GENERAL_STORAGE',10,0,120,1,'BAR_CODE','默认示例仓库','2026-05-16 09:53:01','2026-06-06 21:11:57');
/*!40000 ALTER TABLE `base_warehouse` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `base_zone`
--

DROP TABLE IF EXISTS `base_zone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_zone` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `zone_code` varchar(64) NOT NULL,
  `zone_name` varchar(128) NOT NULL,
  `temperature_type` varchar(32) DEFAULT 'NORMAL',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_zone_code` (`warehouse_id`,`zone_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `base_zone`
--

LOCK TABLES `base_zone` WRITE;
/*!40000 ALTER TABLE `base_zone` DISABLE KEYS */;
INSERT INTO `base_zone` VALUES (1,1,'Z-RECV','收货区','NORMAL','ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,1,'Z-PICK','拣选区','NORMAL','ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(3,1,'Z-COLD','冷藏区','COLD','ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `base_zone` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_contract`
--

DROP TABLE IF EXISTS `billing_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `contract_no` varchar(64) NOT NULL,
  `contract_name` varchar(128) NOT NULL,
  `effective_date` date NOT NULL,
  `expire_date` date DEFAULT NULL,
  `settlement_cycle` varchar(32) NOT NULL DEFAULT 'MONTHLY',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `contract_no` (`contract_no`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_contract`
--

LOCK TABLES `billing_contract` WRITE;
/*!40000 ALTER TABLE `billing_contract` DISABLE KEYS */;
INSERT INTO `billing_contract` VALUES (1,1,1,1,'BC202604150001','重点客户A标准合同','2026-01-01','2026-12-31','MONTHLY','ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `billing_contract` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_rule`
--

DROP TABLE IF EXISTS `billing_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `charge_type` varchar(64) NOT NULL,
  `rule_name` varchar(128) NOT NULL,
  `unit_name` varchar(32) NOT NULL,
  `unit_price` decimal(18,2) NOT NULL DEFAULT '0.00',
  `step_json` text,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_rule`
--

LOCK TABLES `billing_rule` WRITE;
/*!40000 ALTER TABLE `billing_rule` DISABLE KEYS */;
INSERT INTO `billing_rule` VALUES (1,1,'OUTBOUND_ORDER_COUNT','出库单服务费','单',15.00,NULL,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,1,'OUTBOUND_QTY','出库件数服务费','件',0.80,NULL,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(3,1,'INBOUND_QTY','入库件数服务费','件',0.60,NULL,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(4,1,'STORAGE_SNAPSHOT_QTY','库存快照占用费','件',0.10,NULL,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `billing_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_statement`
--

DROP TABLE IF EXISTS `billing_statement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_statement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_no` varchar(64) NOT NULL,
  `customer_id` bigint NOT NULL,
  `statement_month` varchar(16) NOT NULL,
  `total_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `paid_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `statement_status` varchar(32) NOT NULL DEFAULT 'DRAFT',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `statement_no` (`statement_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_statement`
--

LOCK TABLES `billing_statement` WRITE;
/*!40000 ALTER TABLE `billing_statement` DISABLE KEYS */;
/*!40000 ALTER TABLE `billing_statement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_statement_item`
--

DROP TABLE IF EXISTS `billing_statement_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_statement_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_id` bigint NOT NULL,
  `charge_type` varchar(64) NOT NULL,
  `charge_name` varchar(128) NOT NULL,
  `quantity` decimal(18,2) NOT NULL DEFAULT '0.00',
  `unit_price` decimal(18,2) NOT NULL DEFAULT '0.00',
  `amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `biz_no` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_statement_item`
--

LOCK TABLES `billing_statement_item` WRITE;
/*!40000 ALTER TABLE `billing_statement_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `billing_statement_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cargo_code_record`
--

DROP TABLE IF EXISTS `cargo_code_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cargo_code_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inbound_order_id` bigint NOT NULL,
  `inbound_order_item_id` bigint NOT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `operation_type` varchar(64) DEFAULT NULL,
  `operation_code` varchar(64) DEFAULT NULL,
  `location_id` bigint DEFAULT NULL,
  `location_code` varchar(128) DEFAULT NULL,
  `location_name` varchar(128) DEFAULT NULL,
  `zone_name` varchar(128) DEFAULT NULL,
  `cargo_code` varchar(128) NOT NULL,
  `cargo_code_type` varchar(32) NOT NULL,
  `raw_content` text NOT NULL,
  `svg_content` mediumtext,
  `render_format` varchar(64) DEFAULT NULL,
  `render_width` int DEFAULT NULL,
  `render_height` int DEFAULT NULL,
  `external_platform` varchar(64) DEFAULT NULL,
  `external_code` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cargo_code_record_order` (`inbound_order_id`),
  KEY `idx_cargo_code_record_item` (`inbound_order_item_id`),
  KEY `idx_cargo_code_record_code` (`cargo_code`),
  KEY `idx_cargo_code_record_operation` (`operation_code`),
  KEY `idx_cargo_code_record_location` (`location_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cargo_code_record`
--

LOCK TABLES `cargo_code_record` WRITE;
/*!40000 ALTER TABLE `cargo_code_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `cargo_code_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exception_ticket`
--

DROP TABLE IF EXISTS `exception_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exception_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_no` varchar(64) NOT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint DEFAULT NULL,
  `biz_no` varchar(64) DEFAULT NULL,
  `ticket_title` varchar(128) NOT NULL,
  `ticket_content` text,
  `severity` varchar(32) NOT NULL DEFAULT 'MEDIUM',
  `status` varchar(32) NOT NULL DEFAULT 'OPEN',
  `assignee_name` varchar(128) DEFAULT NULL,
  `reporter_name` varchar(128) DEFAULT NULL,
  `approval_status` varchar(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
  `reported_at` datetime DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `closed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ticket_no` (`ticket_no`),
  KEY `idx_exception_ticket_status` (`status`),
  KEY `idx_exception_ticket_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exception_ticket`
--

LOCK TABLES `exception_ticket` WRITE;
/*!40000 ALTER TABLE `exception_ticket` DISABLE KEYS */;
INSERT INTO `exception_ticket` VALUES (1,'EX202604150001',1,'INBOUND_ORDER',1,'IN202604150001','Inbound Putaway Delay','The inbound order has stayed too long after receiving and still waits for putaway.','HIGH','OPEN','仓库主管','系统管理员','NOT_SUBMITTED','2026-05-16 09:53:01',NULL,NULL,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,'EX202604150002',1,'STOCK_TAKE_ORDER',1,'TAKE202604150001','Stock Take Difference','Cycle count found a quantity gap that requires review.','MEDIUM','IN_PROGRESS','库存专员','系统管理员','PENDING','2026-05-16 09:53:01',NULL,NULL,'2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `exception_ticket` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inbound_order`
--

DROP TABLE IF EXISTS `inbound_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inbound_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `order_type` varchar(64) NOT NULL DEFAULT 'PURCHASE',
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `source_no` varchar(64) DEFAULT NULL,
  `expected_arrival_time` datetime DEFAULT NULL,
  `actual_arrival_time` datetime DEFAULT NULL,
  `received_at` datetime DEFAULT NULL,
  `putaway_completed_at` datetime DEFAULT NULL,
  `total_expected_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `total_actual_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `operator_name` varchar(128) DEFAULT NULL,
  `receiver_name` varchar(128) DEFAULT NULL,
  `receiver_phone` varchar(32) DEFAULT NULL,
  `pickup_status` varchar(32) NOT NULL DEFAULT 'NOT_REQUIRED',
  `pickup_code` varchar(64) DEFAULT NULL,
  `pickup_due_at` datetime DEFAULT NULL,
  `last_sms_notice_at` datetime DEFAULT NULL,
  `scan_code` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_inbound_status` (`status`),
  KEY `idx_inbound_warehouse` (`warehouse_id`),
  KEY `idx_inbound_pickup_status` (`pickup_status`,`pickup_due_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inbound_order`
--

LOCK TABLES `inbound_order` WRITE;
/*!40000 ALTER TABLE `inbound_order` DISABLE KEYS */;
INSERT INTO `inbound_order` VALUES (1,'IN202604150001',1,1,1,NULL,'PURCHASE','PUTAWAY_COMPLETED','PO202604150001','2026-05-16 05:53:01','2026-05-16 06:53:01','2026-05-16 06:53:01','2026-05-16 19:13:10',100.00,100.00,'系统管理员',NULL,NULL,'NOT_REQUIRED',NULL,NULL,NULL,NULL,'示例入库单，故意保留未上架用于演示滞留预警','2026-05-16 09:53:01','2026-05-16 19:13:10'),(2,'IN202604150002',1,1,1,NULL,'PURCHASE','PUTAWAY_COMPLETED','',NULL,'2026-05-22 15:27:52','2026-05-22 15:27:52','2026-05-22 15:27:53',1.00,1.00,'系统管理员',NULL,NULL,'PICKED_UP','PICK20260522072058',NULL,NULL,'SCAN20260522072058','','2026-05-22 15:20:59','2026-05-24 13:30:05');
/*!40000 ALTER TABLE `inbound_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inbound_order_item`
--

DROP TABLE IF EXISTS `inbound_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inbound_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_code` varchar(64) NOT NULL,
  `product_name` varchar(128) NOT NULL,
  `batch_no` varchar(64) DEFAULT NULL,
  `production_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `expected_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `actual_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `qualified_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `location_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `cargo_code` varchar(128) DEFAULT NULL,
  `cargo_code_type` varchar(32) NOT NULL DEFAULT 'QR_CODE',
  `cargo_code_content` varchar(2048) DEFAULT NULL,
  `external_platform` varchar(64) DEFAULT NULL,
  `external_code` varchar(255) DEFAULT NULL,
  `putaway_scan_confirmed` tinyint(1) NOT NULL DEFAULT '0',
  `putaway_scan_confirmed_at` datetime DEFAULT NULL,
  `putaway_scan_operator` varchar(128) DEFAULT NULL,
  `putaway_scan_record_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inbound_item_order` (`order_id`),
  KEY `idx_inbound_item_cargo_code` (`cargo_code`),
  KEY `idx_inbound_item_external_code` (`external_code`),
  KEY `idx_inbound_item_cargo_content` (`cargo_code_content`(255)),
  KEY `idx_inbound_item_putaway_scan` (`putaway_scan_confirmed`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inbound_order_item`
--

LOCK TABLES `inbound_order_item` WRITE;
/*!40000 ALTER TABLE `inbound_order_item` DISABLE KEYS */;
INSERT INTO `inbound_order_item` VALUES (1,1,1,'SKU-001','矿泉水 550ml','BATCH-20260415-001','2026-05-16','2027-05-16',100.00,100.00,100.00,1,'默认示例明细','2026-05-16 09:53:01','2026-05-16 09:53:01',NULL,'QR_CODE',NULL,NULL,NULL,0,NULL,NULL,NULL),(2,2,2,'SKU-002','冷藏牛奶 250ml','',NULL,NULL,1.00,1.00,1.00,1,'','2026-05-22 15:20:59','2026-05-22 15:20:59',NULL,'QR_CODE',NULL,NULL,NULL,0,NULL,NULL,NULL);
/*!40000 ALTER TABLE `inbound_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_movement`
--

DROP TABLE IF EXISTS `inventory_movement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_movement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `location_id` bigint DEFAULT NULL,
  `batch_no` varchar(64) DEFAULT NULL,
  `movement_type` varchar(64) NOT NULL,
  `source_type` varchar(64) NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `before_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `change_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `after_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `operator_name` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inventory_movement_product` (`product_id`),
  KEY `idx_inventory_movement_warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_movement`
--

LOCK TABLES `inventory_movement` WRITE;
/*!40000 ALTER TABLE `inventory_movement` DISABLE KEYS */;
INSERT INTO `inventory_movement` VALUES (1,1,1,1,2,'BATCH-20260301-001','INBOUND','INBOUND_ORDER',1,'IN202604150001',200.00,100.00,300.00,'系统管理员','初始化库存流水','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,1,1,1,1,'BATCH-20260415-001','INBOUND','INBOUND_ORDER',1,'IN202604150001',0.00,100.00,100.00,'系统管理员','默认示例明细','2026-05-16 19:13:10','2026-05-16 19:13:10'),(3,1,1,2,1,'','INBOUND','INBOUND_ORDER',2,'IN202604150002',0.00,1.00,1.00,'系统管理员','','2026-05-22 15:27:53','2026-05-22 15:27:53'),(4,1,1,2,1,'','CUSTOMER_PICKUP','INBOUND_ORDER',2,'IN202604150002',1.00,-1.00,0.00,'系统管理员','手持扫码枪取件完成','2026-05-24 13:30:05','2026-05-24 13:30:05'),(5,1,1,1,2,'BATCH-20260301-001','OUTBOUND','OUTBOUND_ORDER',1,'OUT202604150001',300.00,-20.00,280.00,'系统管理员','默认示例明细','2026-05-24 13:30:19','2026-05-24 13:30:19');
/*!40000 ALTER TABLE `inventory_movement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_stock`
--

DROP TABLE IF EXISTS `inventory_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `location_id` bigint DEFAULT NULL,
  `batch_no` varchar(64) DEFAULT NULL,
  `quantity` decimal(18,2) NOT NULL DEFAULT '0.00',
  `locked_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `available_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `last_inbound_at` datetime DEFAULT NULL,
  `last_outbound_at` datetime DEFAULT NULL,
  `last_movement_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stock_product` (`product_id`),
  KEY `idx_stock_warehouse` (`warehouse_id`),
  KEY `idx_stock_owner` (`owner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_stock`
--

LOCK TABLES `inventory_stock` WRITE;
/*!40000 ALTER TABLE `inventory_stock` DISABLE KEYS */;
INSERT INTO `inventory_stock` VALUES (1,1,1,1,2,'BATCH-20260301-001',280.00,20.00,260.00,'2026-04-01 09:53:01','2026-05-24 13:30:19','2026-05-24 13:30:19','2026-05-16 09:53:01','2026-05-24 13:30:19'),(2,1,1,2,3,'BATCH-20260401-002',80.00,0.00,80.00,'2026-05-06 09:53:01',NULL,'2026-05-06 09:53:01','2026-05-16 09:53:01','2026-05-16 09:53:01'),(3,1,1,1,1,'BATCH-20260415-001',100.00,0.00,100.00,'2026-05-16 19:13:10',NULL,'2026-05-16 19:13:10','2026-05-16 19:13:10','2026-05-16 19:13:10'),(4,1,1,2,1,'',0.00,0.00,0.00,'2026-05-22 15:27:53','2026-05-24 13:30:05','2026-05-24 13:30:05','2026-05-22 15:27:53','2026-05-24 13:30:05');
/*!40000 ALTER TABLE `inventory_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_message`
--

DROP TABLE IF EXISTS `notification_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_type` varchar(32) NOT NULL,
  `receiver_name` varchar(128) NOT NULL,
  `receiver_phone` varchar(32) DEFAULT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `biz_type` varchar(64) DEFAULT NULL,
  `biz_id` bigint DEFAULT NULL,
  `message_title` varchar(128) NOT NULL,
  `message_body` text NOT NULL,
  `send_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `response_status` varchar(32) NOT NULL DEFAULT 'NONE',
  `last_error` text,
  `sent_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notification_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_message`
--

LOCK TABLES `notification_message` WRITE;
/*!40000 ALTER TABLE `notification_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `outbound_order`
--

DROP TABLE IF EXISTS `outbound_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbound_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `customer_id` bigint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `carrier_id` bigint DEFAULT NULL,
  `order_type` varchar(64) NOT NULL DEFAULT 'SALES',
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `source_no` varchar(64) DEFAULT NULL,
  `priority_level` varchar(32) NOT NULL DEFAULT 'NORMAL',
  `planned_ship_time` datetime DEFAULT NULL,
  `picking_completed_at` datetime DEFAULT NULL,
  `packed_at` datetime DEFAULT NULL,
  `shipped_at` datetime DEFAULT NULL,
  `total_planned_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `total_shipped_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `operator_name` varchar(128) DEFAULT NULL,
  `logistics_no` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_outbound_status` (`status`),
  KEY `idx_outbound_warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `outbound_order`
--

LOCK TABLES `outbound_order` WRITE;
/*!40000 ALTER TABLE `outbound_order` DISABLE KEYS */;
INSERT INTO `outbound_order` VALUES (1,'OUT202604150001',1,1,1,1,'SALES','SHIPPED','SO202604150001','HIGH','2026-05-16 07:53:01','2026-05-22 11:46:52',NULL,'2026-05-24 13:30:19',20.00,20.00,'系统管理员',NULL,'示例出库单，用于演示出库超时预警','2026-05-16 09:53:01','2026-05-24 13:30:19');
/*!40000 ALTER TABLE `outbound_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `outbound_order_item`
--

DROP TABLE IF EXISTS `outbound_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbound_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_code` varchar(64) NOT NULL,
  `product_name` varchar(128) NOT NULL,
  `batch_no` varchar(64) DEFAULT NULL,
  `planned_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `shipped_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `location_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_outbound_item_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `outbound_order_item`
--

LOCK TABLES `outbound_order_item` WRITE;
/*!40000 ALTER TABLE `outbound_order_item` DISABLE KEYS */;
INSERT INTO `outbound_order_item` VALUES (1,1,1,'SKU-001','矿泉水 550ml','BATCH-20260415-001',20.00,20.00,2,'默认示例明细','2026-05-16 09:53:01','2026-05-24 13:30:19');
/*!40000 ALTER TABLE `outbound_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scan_record`
--

DROP TABLE IF EXISTS `scan_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scan_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `raw_content` text NOT NULL,
  `parsed_code` varchar(255) DEFAULT NULL,
  `scan_format` varchar(64) DEFAULT NULL,
  `content_format` varchar(64) DEFAULT NULL,
  `code_type` varchar(64) DEFAULT NULL,
  `merchant_platform` varchar(64) DEFAULT NULL,
  `matched` tinyint(1) NOT NULL DEFAULT '0',
  `entity_type` varchar(64) DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `source_device` varchar(64) DEFAULT NULL,
  `scanner_interface` varchar(64) DEFAULT NULL,
  `scanner_device_id` varchar(128) DEFAULT NULL,
  `operator_name` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scan_record_created_at` (`created_at`),
  KEY `idx_scan_record_parsed_code` (`parsed_code`),
  KEY `idx_scan_record_entity` (`entity_type`,`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scan_record`
--

LOCK TABLES `scan_record` WRITE;
/*!40000 ALTER TABLE `scan_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `scan_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_take_item`
--

DROP TABLE IF EXISTS `stock_take_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_take_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `take_order_id` bigint NOT NULL,
  `stock_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `system_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `actual_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `diff_qty` decimal(18,2) NOT NULL DEFAULT '0.00',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_take_item`
--

LOCK TABLES `stock_take_item` WRITE;
/*!40000 ALTER TABLE `stock_take_item` DISABLE KEYS */;
INSERT INTO `stock_take_item` VALUES (1,1,1,1,300.00,296.00,-4.00,'DIFF','盘亏示例','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,1,2,2,80.00,80.00,0.00,'MATCHED','盘点正常','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `stock_take_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_take_order`
--

DROP TABLE IF EXISTS `stock_take_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_take_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `take_no` varchar(64) NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `take_type` varchar(32) NOT NULL DEFAULT 'FULL',
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `planned_start_time` datetime DEFAULT NULL,
  `planned_end_time` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `operator_name` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `take_no` (`take_no`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_take_order`
--

LOCK TABLES `stock_take_order` WRITE;
/*!40000 ALTER TABLE `stock_take_order` DISABLE KEYS */;
INSERT INTO `stock_take_order` VALUES (1,'TAKE202604150001',1,1,'CYCLE','COUNTING','2026-05-15 09:53:01','2026-05-17 09:53:01',NULL,'系统管理员','默认盘点任务','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `stock_take_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_code` varchar(64) NOT NULL,
  `menu_name` varchar(128) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `menu_path` varchar(255) DEFAULT NULL,
  `component_name` varchar(128) DEFAULT NULL,
  `icon_name` varchar(64) DEFAULT NULL,
  `menu_type` varchar(32) NOT NULL DEFAULT 'MENU',
  `permission_code` varchar(128) DEFAULT NULL,
  `sort_no` int NOT NULL DEFAULT '0',
  `visible` tinyint(1) NOT NULL DEFAULT '1',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `menu_code` (`menu_code`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'DASHBOARD','Dashboard',NULL,'/dashboard','Dashboard','DataBoard','MENU','dashboard:view',1,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,'MASTER_WAREHOUSES','Warehouses',NULL,'/master/warehouses','ResourcePage','OfficeBuilding','MENU','warehouse:view',10,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(3,'MASTER_LOCATIONS','Locations',NULL,'/master/locations','ResourcePage','Grid','MENU','location:view',11,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(4,'MASTER_OWNERS','Owners',NULL,'/master/owners','ResourcePage','User','MENU','owner:view',12,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(5,'MASTER_SUPPLIERS','Suppliers',NULL,'/master/suppliers','ResourcePage','Van','MENU','supplier:view',13,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(6,'MASTER_CUSTOMERS','Customers',NULL,'/master/customers','ResourcePage','UserFilled','MENU','customer:view',14,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(7,'MASTER_PRODUCTS','Products',NULL,'/master/products','ResourcePage','Goods','MENU','product:view',15,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(8,'INBOUNDS','Inbounds',NULL,'/inbounds','Inbounds','Download','MENU','inbound:view',20,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(9,'OUTBOUNDS','Outbounds',NULL,'/outbounds','Outbounds','Upload','MENU','outbound:view',21,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(10,'STOCKS','Stocks',NULL,'/stocks','Stocks','Box','MENU','stock:view',22,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(11,'ALERTS','Alerts',NULL,'/alerts','Alerts','Bell','MENU','alert:view',23,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(12,'PERMISSIONS','Permissions',NULL,'/permissions','Permissions','Lock','MENU','permission:view',24,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(13,'STOCK_TAKES','Stock Takes',NULL,'/stock-takes','StockTakes','List','MENU','stocktake:view',25,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(14,'BILLING','Billing',NULL,'/billing','Billing','Money','MENU','billing:view',26,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(15,'APPROVALS','Approvals',NULL,'/approvals','Approvals','Select','MENU','approval:view',27,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01'),(16,'EXCEPTIONS','Exceptions',NULL,'/exceptions','Exceptions','Warning','MENU','exception:view',28,1,'ACTIVE','2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(64) NOT NULL,
  `role_name` varchar(128) NOT NULL,
  `role_desc` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'ADMIN','系统管理员','拥有全部权限','ACTIVE','2026-05-16 09:53:00','2026-05-16 09:53:00'),(2,'MANAGER','仓库主管','负责仓库管理与异常处理','ACTIVE','2026-05-16 09:53:00','2026-05-16 09:53:00'),(3,'OPERATOR','仓库作业员','负责日常作业','ACTIVE','2026-05-16 09:53:00','2026-05-16 09:53:00');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1,11,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(2,1,15,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(3,1,14,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(4,1,1,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(5,1,16,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(6,1,8,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(7,1,6,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(8,1,3,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(9,1,4,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(10,1,7,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(11,1,5,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(12,1,2,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(13,1,9,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(14,1,12,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(15,1,13,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(16,1,10,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(32,2,11,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(33,2,15,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(34,2,14,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(35,2,1,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(36,2,16,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(37,2,8,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(38,2,6,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(39,2,3,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(40,2,4,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(41,2,7,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(42,2,5,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(43,2,2,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(44,2,9,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(45,2,13,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(46,2,10,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(47,3,11,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(48,3,1,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(49,3,16,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(50,3,8,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(51,3,9,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(52,3,13,'2026-05-16 09:53:01','2026-05-16 09:53:01'),(53,3,10,'2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(255) NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `role_code` varchar(64) NOT NULL DEFAULT 'ADMIN',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','pbkdf2$120000$ylJjI3r6ffAf79-C0__gpQ$-l9LKRfSjfAfB_EMdHs0oZvodDaf224Q8fYPASOAT0Y','系统管理员','13800000000','admin@example.com','ADMIN','ACTIVE','2026-06-06 21:11:17','初始化管理员账号','2026-05-16 09:53:01','2026-06-06 21:11:17');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1,1,'2026-05-16 09:53:01','2026-05-16 09:53:01');
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_task`
--

DROP TABLE IF EXISTS `warehouse_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `task_type` varchar(64) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `priority_level` varchar(32) NOT NULL DEFAULT 'NORMAL',
  `assignee_name` varchar(128) DEFAULT NULL,
  `assigned_at` datetime DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `due_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `task_no` (`task_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_task`
--

LOCK TABLES `warehouse_task` WRITE;
/*!40000 ALTER TABLE `warehouse_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `warehouse_task` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-06 13:15:35
